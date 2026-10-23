# Binance Spot directory candidate probe - 2026-10-22

This is a bounded transport check for a possible zero-budget market-directory
alternative. It does not establish data coverage, identity mapping, field
quality, current quota, or permission for external display.

## Attempt

Observation date: 2026-10-22 UTC. Exact successful-request time is retained in
the `exchangeInfo.headers` HTTP `Date` header and the local probe output below.

| Route | Exact request | Result | Interpretation |
| --- | --- | --- | --- |
| Command line, direct | `GET https://api.binance.com/api/v3/exchangeInfo?permissions=SPOT` via `curl.exe -L --max-time 20 -sS -D -` | Connection timed out after 20.016 seconds; no HTTP status or response body | Host-to-provider transport failure only; not evidence of an empty directory or Binance outage. |
| Chrome browser session | Navigate to the same public read-only endpoint | CDP `Page.navigate` timed out after 20 seconds; Chrome showed `ERR_CONNECTION_TIMED_OUT` | A second direct route from this host also failed; provider-side availability remains unknown. |
| Command line, system proxy | Same GET via `http://127.0.0.1:7897`, 25-second maximum; raw response retained under `evidence/acquisition/binance-spot-20261022/` | HTTP 200 at `Thu, 22 Oct 2026 04:22:50 GMT`; 17,721,393 bytes; SHA-256 `392395A71DDCBB48FCF88991C1529D728236ABBC35097AA6B1F4072BEA22F5B4` | Snapshot is technically obtainable through this host's configured proxy. This does not prove deployment-host access or entitlement. |

The captured response reports `serverTime=1792642983469` (2026-10-22
04:23:03.469 UTC). Parsing that response produced 3,696 symbol records; 1,387
had `status=TRADING` and `isSpotTradingAllowed=true`; these records contained
517 unique `baseAsset` strings. This is an exchange spot-instrument count, not
a count of unique assets by canonical identity: aliases, wrapped assets,
stablecoins and chain deployments are not reconciled, and records contain no
chain/contract identity. No `JCT` base asset was present in this snapshot.

The `rateLimits` payload advertises a 6,000 request-weight-per-minute limit;
the response header `x-mbx-used-weight: 1280` is observed quota usage metadata,
not the weight attributable to this single call. The exchange information
endpoint response has no quote timestamp for each instrument.

Official documentation access was not completed: the developer-doc URL
redirected to a new path and the response carried `x-amzn-waf-action:
challenge`; no readable body was retained. The official terms URL returned
HTTP 202 with an empty body. Therefore the endpoint's documented filters and
cost semantics, and external display/storage/cache/redistribution/derived-use
permissions remain unverified. Header archives and the exact raw market
response are in `evidence/acquisition/binance-spot-20261022/`.

## Decision

Do not add this endpoint to the runtime source allowlist or use it to populate
the product. A later validation requires readable official documentation and
terms, a same-time CoinAnk reference with matching spot filters, and identity
reconciliation; deployment network access must also be tested. This spot
directory cannot stand in for CoinAnk's combined universe, perpetual-market
instruments, holder or transfer data, contract OI/funding, or liquidation
coverage. No adapter or runtime value was enabled.
