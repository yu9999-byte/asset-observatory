# Official source document review - 2026-10-22 Asia/Shanghai

This is a combined technical and usage-condition review. It does not refresh
market data, grant rights, enable adapters or prove current JCT coverage.
All request timestamps in the archives are UTC.

## CoinLore

- Official API and terms both returned HTTP 200 on 2026-10-21 at 18:08 UTC.
  Archive: `source-documents/20261021T180831904Z/report.json` and its two raw files.
  The first extraction failed after capture; the retained raw bodies were
  hash-checked and replayed offline at 18:13:34 UTC into `extraction.json`.
  An extraction failure is not a failed document HTTP request.
- API URL: https://www.coinlore.com/cryptocurrency-data-api
  SHA256: `ba220d8f9e90da3abb758760046bbff73903acab4ca397472aaa2e8fa95fe755`.
  It says no API key is required and recommends around one request per second.
  This recommendation is not a deployment SLA or a redistribution license.
- Terms URL: https://www.coinlore.com/terms-of-use
  SHA256: `d5063548d96295fc3e60e22fc98046dced118aff59d4a460a1fbe16e0421dd28`.
  Section 6 says: "copying, redistributing, or commercial use of content is
  prohibited without our express written consent".
- Decision: no external display/storage entitlement is inferred from the free
  API presentation. Obtain written permission or clarification of API-specific
  permitted use, or verify an eligible alternative. This does not claim the
  general terms conclusively settle every possible API use.
- The quote sample remains the earlier acquisition ending at
  `2026-10-21T16:26:07.324901800Z`, archived under
  `acquisition/20261021T162534395Z-direct/`. Its provider quote timestamp is
  unknown; the document review does not make that sample current. JCT's contract
  association and missing/invalid supply/cap remain unresolved.

## MEXC candidate

- Official legacy documentation returned HTTP 200 at 18:13 UTC:
  https://mexcdevelop.github.io/apidocs/contract_v1_en/
  Archive: `source-documents/20261021T181334051Z/report.json` and
  `mexc-legacy-docs.raw`/`.txt`. Its meta refresh points to
  https://www.mexc.com/api-docs/futures/update-log . The latter timed out in
  the first document run; no current-document equivalence is claimed.
- Legacy body documents public `GET api/v1/contract/detail` and explicitly uses
  `https://contract.mexc.com/api/v1/contract/detail`. Optional `symbol` is described;
  the stated rate limit is one request per five seconds. The previous probe's
  `api.mexc.com/api/v1/contract/detail/country` did not match this documentation.
  Its retained failure is not evidence about the corrected contract endpoint.
- Corrected Java GET: `acquisition/mexc-corrected-20261022T021624556/report.json`,
  `2026-10-21T18:16:34Z` to `18:16:39Z`, ConnectException, no HTTP response.
- Corrected curl GET: `acquisition/mexc-curl-20261022T021716264/report.json`,
  `18:17:16Z` to `18:17:21Z`, exit 28, connection timeout, HTTP status 0.
  TLS verification stayed enabled; no proxy settings or credentials changed.
- No JCT instrument, units, event price or liquidation sample was retrieved.
  Usage terms have not been verified, so the review exposes `terms:null` and
  no terms link. Connectivity, current documented schema, identity and external
  display/storage eligibility must all be verified before adapter activation.

## Reproduction and limits

Run `node scripts/verify-source-documents.mjs <document-id>` for bounded document
GETs. For the corrected Java endpoint, run Maven with
`-Dtest=AcquisitionProbeLiveTest -Dprobe.live=true -Dprobe.targets=mexc-contracts`
and a new `-Dprobe.output=<directory>`; target selection is allow-listed and
does not activate a production source. Use a fresh archive for each attempt.
Offline replay validates raw SHA256 before parsing and does not execute remote
scripts. Existing replay outputs are intentionally not overwritten.

The failed live probe is an investigation result. Its test-run success means
the evidence was captured, not that the source was reachable or approved.
These observations do not close G01-G06 or any production acceptance gate.
