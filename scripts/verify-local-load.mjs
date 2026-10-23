import { readFile, writeFile } from 'node:fs/promises';
import { cpus, totalmem, platform, release } from 'node:os';
import { performance } from 'node:perf_hooks';

const root = new URL('../', import.meta.url);
const state = JSON.parse(await readFile(new URL('.tools/test-app.json', root), 'utf8'));
const origin = new URL(state.url);
if (state.database !== 'observatory_test' || origin.hostname !== '127.0.0.1' || origin.protocol !== 'http:') {
  throw new Error('Load measurement requires the managed loopback test instance.');
}
const routes = [
  ['/api/markets', value => Array.isArray(value.assets) && envelope(value.market) && envelope(value.derivatives)],
  ['/api/assets/LINK/holders', envelope],
  ['/api/addresses/0x0000000000000000000000000000000000000001/transfers?asset=LINK', envelope],
  ['/api/assets/LINK/liquidations', envelope],
  ['/api/workspace', value => Array.isArray(value.watches) && Array.isArray(value.notes)],
  ['/api/table-preferences/markets', value => value.tableId === 'markets' && value.version > 0 && Array.isArray(value.order)],
];
function envelope(value) {
  return value?.status === 'unavailable' && Array.isArray(value.data) && value.data.length === 0
    && value.fetchedAt === null && typeof value.coverage === 'string' && !!value.reason;
}
const samples = routes.map(() => []);
const errors = routes.map(() => ({ count: 0, first: null }));
async function read(index, measured) {
  const start = performance.now();
  try {
    const response = await fetch(new URL(routes[index][0], origin), { signal: AbortSignal.timeout(10000) });
    if (response.status !== 200) throw new Error(`HTTP ${response.status}`);
    const value = await response.json();
    if (!routes[index][1](value)) throw new Error('Response shape or expected missing-source semantics differs');
  } catch (error) {
    if (!measured) throw error;
    errors[index].count++;
    errors[index].first ??= error.message;
  } finally {
    if (measured) samples[index].push(performance.now() - start);
  }
}
for (let round = 0; round < 3; round++) {
  for (let index = 0; index < routes.length; index++) await read(index, false);
}
const startedAt = new Date().toISOString();
const start = performance.now();
const durationMs = 60000;
const readers = 20;
console.log('Measuring 20 local readers for 60 seconds; sources must remain disabled.');
await Promise.all(Array.from({ length: readers }, async (_, reader) => {
  let index = reader;
  while (performance.now() - start < durationMs) {
    await read(index++ % routes.length, true);
  }
}));
const elapsedMs = performance.now() - start;
function stats(values) {
  const sorted = [...values].sort((a, b) => a - b);
  const percentile = p => Number(sorted[Math.max(0, Math.ceil(sorted.length * p) - 1)].toFixed(3));
  return { requests: sorted.length, p50Ms: percentile(.5), p95Ms: percentile(.95), p99Ms: percentile(.99), maxMs: percentile(1) };
}
const perRoute = routes.map(([path], i) => ({ path, ...stats(samples[i]), errors: errors[i].count, firstError: errors[i].first }));
const report = {
  startedAt, checkedAt: new Date().toISOString(), database: state.database, url: state.url,
  readers, requestedDurationMs: durationMs, elapsedMs: Number(elapsedMs.toFixed(3)),
  workload: '20 closed-loop readers, sequential round-robin per reader; full JSON body read and validated; 3 warm-up rounds excluded.',
  environment: { platform: platform(), osRelease: release(), cpu: cpus()[0]?.model, logicalCpus: cpus().length, memoryBytes: totalmem(), node: process.version, backend: 'Java 21.0.12.1+1 / Spring Boot 3.5.16', database: 'MySQL 8.4.13' },
  overall: stats(samples.flat()), perRoute,
  passed: perRoute.every(route => route.errors === 0 && route.p95Ms < 300) && elapsedMs >= durationMs,
  coverage: 'Warm local sources-disabled API and MySQL reads only; not enabled-source caching, cold upstream budgets, browser startup or production capacity.',
};
await writeFile(new URL('docs/evidence/local-load-verification.json', root), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
if (!report.passed) process.exitCode = 1;
