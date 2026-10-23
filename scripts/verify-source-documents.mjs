import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';
import { join, resolve } from 'node:path';

const require = createRequire(new URL('../frontend/package.json', import.meta.url));
const { JSDOM } = require('jsdom');
const targets = [
  ['coinlore-api', 'https://www.coinlore.com/cryptocurrency-data-api'],
  ['coinlore-terms', 'https://www.coinlore.com/terms-of-use'],
  ['mexc-legacy-docs', 'https://mexcdevelop.github.io/apidocs/contract_v1_en/'],
  ['mexc-current-docs', 'https://www.mexc.com/api-docs/futures/update-log'],
];
function extract(body, url) {
  const dom = new JSDOM('', { url });
  try {
    const doc = new dom.window.DOMParser().parseFromString(body.toString('utf8'), 'text/html');
    const details = { title: doc.title, metaRefresh: doc.querySelector('meta[http-equiv="refresh"]')?.content ?? null,
      links: [...new Set([...doc.querySelectorAll('a[href]')].map(a => new URL(a.getAttribute('href'), url).href))]
        .filter(href => /api-docs|terms|license|contract\/detail|commercial/i.test(href)).slice(0, 120) };
    doc.querySelectorAll('script, style, nav, footer, header').forEach(node => node.remove());
    const text = (doc.querySelector('main') ?? doc.body).textContent.replace(/\s+/g, ' ').trim();
    details.excerpts = [];
    for (const match of text.matchAll(/commercial|redistribut|license|permission|attribution|30 requests|per second|\/api\/v1\/contract\/detail/gi)) {
      details.excerpts.push(text.slice(Math.max(0, match.index - 180), match.index + 550));
      if (details.excerpts.length === 22) break;
    }
    return { details, text };
  } finally { dom.window.close(); }
}
if (process.argv[2] === '--replay') {
  const input = resolve(process.argv[3]);
  const original = JSON.parse(readFileSync(join(input, 'report.json'), 'utf8'));
  const replay = { extractedAt: new Date().toISOString(), networkRequests: 0, requests: [] };
  for (const record of original.requests.filter(row => row.httpStatus === 200 && row.rawFile)) {
    if (!targets.some(([id, url]) => id === record.id && url === record.url) || record.rawFile !== `${record.id}.raw`)
      throw new Error('Unexpected archived target');
    const body = readFileSync(join(input, record.rawFile));
    if (createHash('sha256').update(body).digest('hex') !== record.bodySha256) throw new Error('Archive hash mismatch');
    const { details, text } = extract(body, record.url);
    replay.requests.push({ id: record.id, url: record.url, bodySha256: record.bodySha256, ...details });
    writeFileSync(join(input, `${record.id}.txt`), text, { flag: 'wx' });
  }
  writeFileSync(join(input, 'extraction.json'), JSON.stringify(replay, null, 2), { flag: 'wx' });
  console.log(JSON.stringify(replay));
  process.exit(0);
}
const requested = process.argv.slice(2);
if (requested.some(id => !targets.some(([candidate]) => id === candidate))) throw new Error('Unknown document target');
const output = fileURLToPath(new URL(`../docs/evidence/source-documents/${new Date().toISOString().replace(/[-:.]/g, '')}/`, import.meta.url));
mkdirSync(output, { recursive: true });
const report = { startedAt: new Date().toISOString(), purpose: 'Official document verification only',
  productionApproved: false, method: 'GET', transport: 'curl direct, verified TLS, no redirects', requests: [] };
for (const [id, url] of targets) {
  if (requested.length && !requested.includes(id)) continue;
  const record = { id, url, startedAt: new Date().toISOString() };
  report.requests.push(record);
  try {
    const response = execFileSync('curl.exe', ['--silent', '--show-error', '--connect-timeout', '5',
      '--max-time', '18', '--max-filesize', '2000000', '--write-out', '\n__METADATA__%{json}', url],
    { timeout: 20000, maxBuffer: 2100000, windowsHide: true });
    const boundary = response.lastIndexOf('\n__METADATA__');
    if (boundary < 0) throw new Error('Missing curl metadata');
    const body = response.subarray(0, boundary);
    const metadata = JSON.parse(response.subarray(boundary + '\n__METADATA__'.length).toString());
    Object.assign(record, { httpStatus: metadata.http_code, elapsedSeconds: metadata.time_total,
      bytes: body.length, bodySha256: createHash('sha256').update(body).digest('hex'), rawFile: `${id}.raw` });
    writeFileSync(join(output, record.rawFile), body, { flag: 'wx' });
    record.fetchStatus = metadata.http_code === 200 ? 'captured' : 'http_error';
    if (metadata.http_code === 200) {
      const { details, text } = extract(body, url);
      Object.assign(record, details);
      writeFileSync(join(output, `${id}.txt`), text, { flag: 'wx' });
    }
  } catch (error) {
    if (!record.fetchStatus) record.fetchStatus = 'transport_error';
    else record.extractionStatus = 'error';
    record.error = String(error.stderr?.toString() || error.message).slice(0, 1200);
  }
  record.endedAt = new Date().toISOString();
  writeFileSync(join(output, 'report.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(record));
  await new Promise(resolve => setTimeout(resolve, 1000));
}
report.endedAt = new Date().toISOString();
writeFileSync(join(output, 'report.json'), JSON.stringify(report, null, 2));
console.log(`Evidence: ${output}`);
