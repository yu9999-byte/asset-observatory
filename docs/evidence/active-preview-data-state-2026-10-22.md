# Active preview data-state check - 2026-10-22

## Observation

At 2026-10-22 04:45 UTC, read-only GET requests to the existing local preview
on `127.0.0.1:8080` returned:

- `/actuator/health`: `UP`.
- `/api/markets/query?limit=10`: `total=7`, all seven market measurements and
  `sourceAt` values null, but `completeness="complete"`. The same response says
  the configured seven-asset directory "is not the market universe" and says
  the market source is unavailable. JCT has an empty instrument mapping.
- `/api/sources`: CoinLore, MEXC, Routescan, Kraken, Blockscout legacy and OKX;
  all show `enabled=false` and `productionApproved=false`. The running response
  does not include the recently reviewed DeBank source record.

This is a live runtime observation, distinct from the current source tree and
the earlier contract test that expects `partial` for the unreconciled catalog.
It demonstrates that health status only proves the process responds; it does
not prove directory completeness, fresh code, live source ingestion, or
production-approved data. No source was enabled and no state was changed.

## Decision

Do not present the current preview as an accurate demonstration of source
coverage. Keep the seven-row market universe and all provider measurements
described as incomplete/missing. The process was not restarted because that
would replace the user's active preview. Before using the preview for product
acceptance, start a separate instance from the verified current build and
confirm it reports `partial` plus the current source registry; then conduct
fresh provider checks only for approved sources.

## Recheck — 2026-10-22 05:15 UTC

Read-only requests were repeated against both existing Java listeners,
`127.0.0.1:8080` and `127.0.0.1:8081`:

- `GET /api/markets/query?page=0&size=10` returned HTTP 200 on both ports,
  `total=7`, `completeness="complete"`, and seven rows with no `lastPrice`,
  `open24h`, `change24hPct`, `volume24hBase`, `turnover24hQuote`, or `sourceAt`
  values. The first row was BTC / `XXBTZUSD`, with all six measurements/time
  fields null.
- `GET /api/sources` returned six records on both ports: CoinLore, MEXC
  candidate, Routescan, Kraken, Blockscout legacy and OKX (excluded). The
  current Java source contains eight records, including DeBank Cloud and
  CoinDesk Data API; the running response omits both.
- Both listeners belong to `java.exe` processes running the same
  `target/asset-observatory-0.1.0.jar`. The JAR last-write time was
  2026-10-22 12:48:06 Asia/Shanghai, while `ResearchService.java` was modified
  at 13:10:48 Asia/Shanghai. The running artifact therefore predates the
  current source file.
- `/` and `/api/health` timed out within eight seconds on both ports, while the
  market and source endpoints responded. No process was restarted or changed.

The current source implementation returns `completeness="partial"` for this
unreconciled configured catalog, and the existing OpenAPI contract test asserts
that behavior. The runtime mismatch is an artifact/deployment issue; passing
source tests does not make the running preview current. Until a separate
current-build instance is started and checked, neither preview is suitable for
acceptance. The observed response also proves that no live prices were being
shown at recheck time; it does not prove the upstream providers had no data.

## Recheck — 2026-10-22 05:21 UTC

Using the current plural route from the API contract, read-only requests to
both `127.0.0.1:8080` and `127.0.0.1:8081` returned HTTP 200 for
`/api/markets`, `/api/sources`, and `/actuator/health` (`UP`). The earlier
13:20 check of `/api/market` was a singular-path probe and its timeout/404
results are not evidence that either service was unavailable.

- `/api/markets` returned seven configured assets and empty `market.data` and
  `derivatives.data`; both envelopes had `status="unavailable"`, null fetch
  and source timestamps, and the reason `来源未启用或自用许可评估尚未确认`.
- `/api/sources` returned six source records on each listener. This still
  differs from the current source registry, which includes DeBank Cloud and
  CoinDesk Data API as disabled candidates.
- An isolated copy of the current source, docs and frontend assets compiled and
  packaged successfully as a Spring Boot executable JAR under
  `.tools/goal-source-build-20261022/target/`; archive inspection confirmed
  `BOOT-INF/lib/` and `BOOT-INF/classes/static/index.html`. Tests were
  deliberately skipped for this packaging check.
- The full Maven test run executed 83 tests: 81 passed, one live probe was
  skipped, and `ExternalSecurityTest.registrationSupportsTheContractMaximumPasswordWithoutTruncation`
  failed. The API contract permits 128 characters but BCrypt rejects a 128
  byte ASCII password at its 72-byte limit. This authentication-contract issue
  is outside the data-source goal and remains open; `OpenApiContractTest`'s
  six tests passed.

Neither listener was restarted. Runtime remains responsive, but contains no
live market or derivatives measurements and does not match the latest source
registry. The external-user product still has no source approved for runtime
use under the current zero-budget and unconfirmed third-party display rights.

