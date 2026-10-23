# Requirement traceability (living evidence, not a completion claim)

Current phase: source and platform feasibility / first implementation. User's screenshot scope is
authoritative in specification v2. Local MySQL migration and isolated restore verified. No public release.

Deployment contract increment (2026-10-22): `docs/deployment.md`, `Dockerfile`,
`docker-compose.yml`, CI workflow, and `scripts/{backup,restore}-mysql.ps1` are
checked in. PowerShell parsing and Maven/frontend checks passed; Docker runtime
and production recovery remain unverified as recorded in
`evidence/deployment-contract-verification-20261022.json`.

| Requirement | Current implementation evidence | Outstanding |
| --- | --- | --- |
| R01 | Kraken adapter, ProviderMappingTest; Java CoinLore quote capture and missing-field normalization in DiscoveryMappingTest/AcquisitionEvidenceTest | eligible live external-use adapter; quote freshness, contract mapping and independent price reconciliation |
| R02-R03 | precision/cursor mapping tests, holders/address UI; JCT identity/decimals verified; earlier29/30 holder comparison; four Java indexed transfers uniquely match raw logs; 2026-10-22 02:22 UTC Blockscout/Ethplorer recheck ended in host `ConnectException` before HTTP response (`evidence/holder-source-recheck-2026-10-22.md`) | eligible production chain source; holder discrepancy, independent-source log reconciliation and complete live flow |
| R04 | native OI visible with unit unknown; CoinAnk JCT page discovery lists six venues but API, units and external rights are unverified (`evidence/coinank-jct-page-review-2026-10-22.md`) | official JCT venue/instrument and unit mapping; entitled API sample and no inferred USD coverage |
| R05 | documented Kraken history filter | actual liquidation-event reconciliation; continuity |
| R06-R07 | watch/note API; separate private project name/info/custom text; real MySQL isolation/conflict tests; desktop/mobile reload and hide/reopen E2E; V6 migration, project restore and restart evidence; V9 managed accounts with public researcher registration, admin provisioning/reset/disable, role checks and secret-safe responses; `AccountAdminIntegrationTest`, `ExternalSecurityTest` | rate limits, email verification, bot controls, abuse monitoring, independent-account persistence/restart drill; production durability |
| R08 | DataTable tests: exact sort, filtering, CSV escaping; persistent column order/visibility/sort/page size; desktop/mobile E2E; table query/completeness contract in `docs/table-query-contract.md` | current market directory is fixed at seven internal entries, not the user's CoinAnk 800+ reference universe; directory count/scope and provider reconciliation are unverified; holders/transfers are one unstable upstream page; no complete ranking or export claim. DA11 now requires a timed, identity-based universe comparison before completeness is claimed. |
| R09-R10 | envelopes; dated source-review metadata distinct from activation/rights; per-key single-flight; bounded follower wait/concurrency/body; total transport deadline and cancellation; stale timestamp tests | enabled-source live outage and retrieval-budget drill |
| R11-R12 | loopback gate, real Basic authentication/session-bound CSRF regression tests, health; local MySQL restore SHA256 equality; container/backup contract artifacts; V9 account lifecycle and admin-only routes described in `docs/account-management.md` and `docs/openapi.json` | public perimeter, Docker runtime, database crash/restart, rollback and production recovery evidence |
| R13-R19 | original scope captured, metrics requirements frozen | valid supply/history/funding/labels; all feature acceptance pending |
| R20-R24 | owner-scoped MySQL watches/notes/table preferences; two-user tests; layout conflict E2E; local backup/restore and app restart; policy-gated holder journal V4-V5 with immutable policy evidence and MySQL tests; bounded policy-aware holder cleanup V8; V9 managed account persistence; CI and deployment contract artifacts | market/transfer history, ingestion workers, backfill/reorg, Docker runtime/recovery, independent-account restart and production drills |

Initial red tests: frontend DataTable import unresolved; Java missing ResearchService/Metrics/Cursor
before implementation. Initial green: 13 Java tests + 2 frontend tests. These initial Java tests used
H2 before the user's MySQL decision and therefore DO NOT establish MySQL correctness.

## Verified local increment (2026-10-21)

- Java: 40 tests passed, with integration coverage against isolated MySQL 8.4.13, including owner isolation,
  optimistic note/layout conflicts, preference validation, CSRF and the static
  JavaScript routing regression.
- API contract: `docs/openapi.json` (OpenAPI 3.1.0) covers registered business
  routes and health. Five tests validate the official OpenAPI schema, component
  schemas, route inventory, actual MVC responses and negative provenance/precision
  cases. The official schema fixture and its SHA256 provenance are checked in;
  contract validation runs offline.
- External authentication: two real-credential tests prove API/static/health
  protection and session-bound CSRF. They first exposed a missing `{bcrypt}`
  encoder prefix; the corrected configuration passes. A missing required query
  parameter also first exposed an inconsistent error response; it now returns
  the documented 400 `VALIDATION_FAILED` body. These are software security checks,
  not evidence of TLS, production identity management or a deployed perimeter.
- R10 deterministic tests: slow cache key does not block another; same-key wait
  and active-key capacity are bounded; stale fallback retains original fetchedAt;
  source deadline cancels pending body completion and releases concurrency slots;
  third caller is rejected under saturation; pacing consumes the total budget;
  illegal targets never reach transport; oversized streaming bodies are cancelled.
- Frontend: 6 unit tests passed; TypeScript/Vite production build passed. Preference
  hydration sends no writes; rejected saves retain confirmed settings and require reload.
- Playwright: 4 tests passed, desktop 1440x1000 and iPhone 13 emulation, using the
  separate `observatory_test` database. Screenshots were visually inspected.
- Browser assertions: market -> LINK holders -> manually entered test address;
  watch survives reload; note saves, hides, reopens and survives reload; script
  text stays inert; no page errors or document horizontal overflow.
- R23 browser assertions: column move/hide, descending sort and 30-row page size
  survive reload; search text stays transient; a stale second tab receives 409,
  retains its confirmed settings and explicitly reloads the newer version.
- Regressions first observed: SPA asset routes returned HTML for JS bundles,
  causing a blank page; note-version refresh erased the save acknowledgement.
  Both are fixed. These browser tests do not prove live-source acceptance; JCT
  identity has separate primary-source evidence, not full-flow acceptance.
- Local restore: `docs/evidence/mysql-restore-verification.json`; a nonempty test
  workspace plus disabled policy/holder journal were dumped and restored into a
  separate database with equal full schema/data SHA256. Additional assertions
  verify exact uint256 maximum/zero, NULL source time, UTC microseconds,
  checkpoint/version and immutable JSON policy evidence. Synthetic fixture hashes
  do not prove worker replay or live-source identity. The source was not overwritten.
- App restart: `docs/evidence/local-restart-verification.json`; a nonempty test
  watch/note workspace and market layout retained identical canonical response
  SHA256 and versions across Java process termination/restart. MySQL kept running;
  this does not establish database crash recovery.
- Local load: `docs/evidence/local-load-verification.json`; 20 closed-loop readers
  for 60 seconds over six APIs, full JSON/explicit missing-source validation,
  per-route p95 <300ms and zero errors. Sources disabled; enabled-source cache
  and cold retrieval performance remain unverified. Hardware is in the report.
- Holder journal: nine MySQL tests pass for policy denial/revocation/expiry,
  precision, unknown source time, transactional rollback, exact retry/content
  mismatch, lease expiry/concurrency, bounded validation and immutable policy
  revision evidence. Initial tests failed before HistoryStore existed; the audit
  regression then failed before V5 added policy evidence. No production policy,
  scheduler, historical API or continuous coverage is claimed.
- Flyway successfully applied all five migrations on MySQL 8.4.13, but its bundled
  version warns its officially tested MySQL range ends at 8.1. This is a production
  compatibility review item, not a vendor certification claim.

Browser screenshots: `frontend/test-results/research-flow-local-MySQL--db141-licitly-missing-source-data-{desktop,mobile}/`.
Layout screenshots: `frontend/test-results/research-flow-R23-table-la-0093a-ng-tabs-cannot-overwrite-it-{desktop,mobile}/`.
Sources remain disabled pending documented usage rights and coverage validation.

## Three-page interface realignment (2026-10-21)

Authoritative input: the user's restated Goal and original discussion image.
Contract: `docs/ui-realignment.md`, UI01-UI12, still subordinate to R01-R24.

- White/gray compact tables and fine dividers replace large metric cards and
  excessive title spacing. Desktop table row <=48px is a browser assertion.
- Markets exposes cap, FDV, volume, OI, funding and change columns plus four
  ranking sections. Missing fields/rankings have explicit reasons, not invented
  totals, plots or zero-valued proportion bars.
- Asset summary uses actual selected-asset API values when present. Loaded and
  custom/watched address scopes differ; an out-of-page watch stays visible with
  unknown quantity. Top30 and multisig are disabled pending coverage/identity evidence.
- Transfers exposes From/To and event price/value, real direction filtering and
  24H/3D/7D period selection. Partial cursor pages never become complete-period
  totals; exchange attribution remains unavailable.
- Notes preview saved text; pin/fold controls preserve drafts; save/hide and
  reload persist through MySQL. Identity columns pin when first in visible order.
- Existing layout settings normalize new/removed columns on read, retaining valid
  order/visibility, page size and version; GET does not mutate stored JSON.
- Red frontend regressions exposed missing note pin/fold, summary data reuse and
  event/filter columns. A MySQL red regression exposed legacy column mismatch.
  Initial visual edits preceded these tests; this is explicitly not a claim that
  every layout edit followed test-first development.
- Verified: 42 Java tests, 11 frontend tests, TypeScript/Vite production build,
  8 Playwright cases across desktop 1440x1000 and iPhone 13. First browser pass
  found a 50px row; line-height fix passed the <=48px requirement on retest.
  No page errors or document horizontal overflow in the asserted flows.
- Final screenshots visually reviewed: market, holders and transfers on desktop,
  holders/transfers on mobile. Preserved under `docs/evidence/ui-realignment/`.
  Both 8080 (personal preview) and 8081 (isolated test instance) restart on MySQL,
  loopback only. The existing in-app 8080 page was refreshed to the new bundle.

Still open: source permissions, live catalog coverage, complete Top30, backed
multisig/exchange labels, supply and proportion bars, historical quantity/USD
changes, rankings and statistics, continuous event monitoring, project-name/info
fields, global backend query/sort protocol, and production deployment/recovery.
An honest missing state or disabled filter does not pass these feature gates.
No new real-data reconciliation or production drill is claimed in this UI increment.

## Acquisition-first increment (2026-10-22)

User clarification: acquire and verify data first, then choose query/cache/storage;
MySQL is used when persistence is required. SDD contract DA01–DA08 and R09 review
metadata are in `data-acquisition.md`; ADR-006 captures the resulting decision.
`execution-plan.md` retains all eight stages and the original feature backlog.

- Two opt-in Java runs archive raw SHA256 evidence: full candidate run4/16
  HTTP200 captures; core reconciliation run6/7 captures. Detailed failures,
  quote fields, rights references and G01–G06 unlocks are in
  `evidence/data-acquisition-2026-10-22.md`. No new runtime provider is enabled.
- CoinLore7-asset sample preserves invalid/missing JCT supply as null and source
  time as unknown. Four historical indexed JCT transfers match seven raw logs
  across four sampled blocks, uniquely by exact identity/amount/time fields.
  This is same-provider matching, not independent or continuous acceptance.
- TDD: seven parser tests first failed compilation before DiscoveryMapping
  existed; the DA09 frontend regression failed before the source review display.
  Four additional offline evidence tests replay retained real samples and hashes;
  their purpose is regression/audit, not a claim of test-first source discovery.
- Verified this increment:53 Java tests pass against isolated MySQL where needed,
  with zero failures/errors; the opt-in live test is separately skipped in the
  ordinary54-test inventory. Twelve frontend unit tests and TypeScript/Vite
  production build pass. OpenAPI contract includes the actual optional review
  object; source evidence does not activate the provider or call its transport.
- Updated local preview and isolated test instance from the rebuilt JAR. Only
  verified project Java processes were stopped; MySQL kept running. No public
  deployment, new recovery/load drill or production acceptance is claimed here.
- Ten Playwright cases pass on desktop1440x1000 and iPhone13: dated review
  reachability/activation separation, source/time/coverage display, reload,
  compact three-page flow, notes/watches and table-preference conflict handling.
  Both source-page screenshots were visually reviewed, with no asserted page
  errors or horizontal document overflow. Preserved images:
  `evidence/acquisition-ui/sources-desktop.png` and `sources-mobile.png`.
  These cases use the rebuilt Java service and isolated real MySQL; they do not
  supply actual providers to the runtime or establish live full-flow acceptance.

Remaining gates are unchanged: external-use entitlement, eligible current holder
coverage, JCT venue identity/units, historical windows, complete backend queries,
project records, ingestion/reorg/replay, managed accounts and production drills.

## Private project records increment (2026-10-22)

The project-record gap above is resolved for the currently supported asset and
Ethereum-address scopes. PR01–PR08 in `project-records.md` defines the contract
before implementation: three separate private user fields, one owner/entity key,
one version, bounded UTF-16 lengths and backwards-compatible omitted/null inputs.
This does not resolve managed accounts, cross-chain expansion or source labels.

- Four new MySQL behavior tests failed before implementation (missing fields and
  accepted oversize input), then passed after V6 and the conditional SQL update.
  Existing clients preserve project fields; explicit empty strings clear them.
  Tests cover two users, asset/address separation, stale versions, Unicode limits
  and legacy inserts. No fake source measurements are involved.
- Two new editor tests failed before project controls existed. A subsequent
  regression exposed automatic closure after saving from the watch workspace;
  navigation/hydration restoration now leaves the current editor open on save.
  Fifteen frontend tests pass; TypeScript/Vite build passes.
- Full Java suite passed57 tests with one opt-in probe skipped. After adding one
  further actual-response contract test, all6 OpenApiContractTest cases passed.
  Thus current reports cover58 passing tests and one skipped probe, with no
  failure/error; the final added test was targeted, not another full-suite run.
- Twelve Playwright cases pass on desktop1440x1000 and iPhone13. They exercise
  all three fields, pin/fold draft retention, hide/reload/reopen, hover title,
  workspace editing, address separation, inert script text and viewport bounds.
  Both editor screenshots were visually inspected.
- Exact V1–V5 SQL with two synthetic legacy notes followed by exact V6 preserves
  original-column SHA256, defaults both new fields to empty and accepts a Unicode
  project update. Evidence: `evidence/project-records-migration-20261021164825067.json`.
- Preview upgrade: `evidence/project-records-preview-upgrade-20261021165244686.json`.
  The personal preview had zero notes/watches, so its equal hash alone is weak
  migration evidence; the separate nonempty isolated migration above proves the
  retained-column behavior. Both loopback instances run the rebuilt JAR.
- Backup/restore: `evidence/mysql-restore-verification-20261021165431466.json`.
  Schema/data dump SHA256 matches in a separate restore database; exact Unicode
  name, info, custom text, visibility/version/time and holder journal are checked.
  Source database is not overwritten; synthetic fixtures are isolated test state.
- Application restart: `evidence/local-restart-verification-20261021165432696.json`.
  Three notes including two project records, one watch and table layout retain
  equal canonical SHA256 and versions. MySQL stays running; this is not database
  crash recovery or production recovery. Dated reports preserve earlier evidence.

UI evidence: `evidence/project-records-ui/editor-desktop.png` and
`evidence/project-records-ui/editor-mobile.png`. Data sources remain disabled,
and the original real-data, monitoring, full-query and production gates remain.

## Regression rerun (2026-10-22)

After the project-record editor route-restoration fix, the current checkout was
retested: all 16 frontend unit tests pass; the TypeScript/Vite production build
passes; all 59 Java tests pass against the isolated MySQL 8.4 database, with one
opt-in live acquisition probe skipped by default; and all 12 Playwright cases
pass across desktop and mobile. This rerun confirms local software behavior only.
It does not change the outstanding external-use entitlement, complete-data,
continuous-ingestion, global-query, managed-account, or production deployment and
recovery gates documented above. Flyway still warns that its bundled release's
tested MySQL range ends at 8.1.

## Server market query integration (2026-10-22)

- `GET /api/markets/query` is now the request path used by the行情表 for
  server-side search and persisted sort/direction/page-size. The UI filters its
  rendered rows to the returned directory page and keeps the existing source
  coverage/missing-value states.
- OpenAPI and API contract docs include `change24hPct` as an allow-listed sort
  field. The query still describes only the seven configured assets, with nulls
  last in either direction and two-minute MySQL-persisted cursors.
- Verification: frontend unit tests 16/16, Vite production build, Java Maven
  suite 63/63 (one opt-in live probe skipped), and Playwright 14/14 across
  desktop 1440x1000 and iPhone 13. The new R08 case observed a real `q=JCT`
  response with `total=1` and both ascending/descending server sort requests.
- This increment does not unlock external display rights, complete live
  provider coverage, holder/transfer global queries, continuous ingestion, or
  production database availability/recovery.

## MySQL market query snapshots (2026-10-22)

R08 cursor state is persisted in `market_query_snapshots` by Flyway V7. The
snapshot contains the ordered seven-asset directory and source envelope, is
bound to normalized query parameters, expires after two minutes and is capped
at 32 rows. Three MySQL integration tests verify cross-instance continuation,
condition mismatch, expiry deletion, deterministic concurrent reads and bounded
cleanup. The full Java suite (63 tests, one opt-in live probe skipped), frontend
unit suite (16 tests), build and 14 Playwright cases pass. Evidence is in
`evidence/market-query-snapshot-verification-20261022.json`.

This closes the process-local cursor limitation only. It does not establish
database crash recovery, source entitlement, complete market/holder/transfer
coverage or production deployment acceptance.

## Transfer ingress and source review increment (2026-10-22)

- TX01-TX07 in `transfer-ingress.md` specify strict transaction identity,
  JavaScript-safe integer bounds, decimal-string uint256 precision, nullable
  event time and identical-only deduplication. A malformed/conflicting row
  rejects the new page. Seven regression tests first failed, then passed:
  `evidence/transfer-ingress-red.xml` and `transfer-ingress-green.xml`.
  API/OpenAPI contracts and provider fixtures match these rules. This is page
  validation only; persisted events, block hashes, finality and reorg handling
  remain open.
- DA10 in `data-acquisition.md` specifies that dated rights reviews cannot
  refresh quote time or activate providers. Backend and frontend regressions
  failed before implementation and passed afterwards. Backend evidence:
  `evidence/source-terms-review-red.xml` and `source-terms-review-green.xml`.
  Unverified terms URLs are null and appear without a guessed link. The source
  page distinguishes connectivity records from the latest review date.
- CoinLore official API/terms returned HTTP 200. Captured section 6 requires
  express written consent for redistribution/commercial use; consent or an
  applicable clarification remains absent. The earlier quote acquisition time
  remains unchanged. MEXC's corrected legacy-documented contract endpoint
  failed in both Java and curl with no HTTP response. The previous wrong URL
  is not evidence about the corrected endpoint. Raw archives, timestamps,
  limitations and unlocks: `evidence/source-documents-review-2026-10-22.md`.
- Full Maven suite: 71 tests total, 70 passed, one opt-in live probe skipped,
  zero failures/errors. Frontend unit tests: 17/17 passed; TypeScript/Vite build
  and Maven package passed. Two targeted source-review Playwright cases passed
  on desktop 1440x1000 and iPhone 13. These are targeted browser cases, not a
  rerun of the earlier complete browser suite. Screenshots were visually
  inspected; the cases check page errors and horizontal overflow.
- Both loopback previews run the rebuilt JAR; only the verified preview Java
  processes were stopped during packaging, while MySQL stayed running. The
  8080 health and actual source metadata were verified after restart.
  Verification and preserved screenshots:
  `evidence/transfer-source-verification-20261022.json`,
  `evidence/transfer-source-increment-20261022.md`, and
  `evidence/source-review-ui/sources-desktop.png` / `sources-mobile.png`.

All providers remain disabled. These checks prove local software behavior and
dated investigations, not entitled live-data acceptance. Current fixed-block
Top30, JCT derivatives, event-time prices/history, holder/transfer global queries,
continuous ingestion, managed external accounts and production drills remain
open. Docker is unavailable; no new deployment or recovery drill is claimed.

## Holder retention cleanup increment (2026-10-22)

HC01-HC04 in `data-model.md` specify cleanup by each source policy's retention
age, an oldest-first whole-batch row bound, aggregate audit evidence, scheduled
execution and the current absence of any gap/reorg reference tables. A MySQL
test first failed to compile because the operation was missing. The MySQL
integration suite now covers policy age including disabled policy, retention of
fresh rows, repeat/bound behavior, input limits and transaction rollback.
Verification: `mvn test` passed 75 tests, with one opt-in live probe skipped,
zero failures and zero errors; `HistoryStoreIntegrationTest` passed 13/13 on
MySQL 8.4.13 and Flyway validated/applied schema version 8. This is local
software evidence only. The periodic cleanup's production scheduling, audit
operations and deployment recovery remain unverified. The packaged JAR was
rebuilt and both loopback previews were restarted with sources disabled; startup
logs confirm Flyway schema version 8 for `observatory` and `observatory_test`,
and both `/actuator/health` endpoints report `UP`.

## DEX quote alternative transport check (2026-10-22)

Following the DA07 fallback requirement, one bounded direct HTTPS GET was made
to each provider's JCT Ethereum endpoint. DexScreener and GeckoTerminal both
timed out during connect after 8 seconds, with no HTTP response or body. Raw
request metadata and exact URLs are retained at
`evidence/acquisition/jct-dex-alternatives-20261022/report.json`. This narrows
the current host's network feasibility only; it says nothing about provider-side
availability, dataset contents or rights. Both sources remain disabled and
unapproved. Resume after a permitted network route or browser authorization is
available, then verify live fields and official terms separately.

## Current source recheck (2026-10-22)

The opt-in Java probe captured a fresh CoinLore response (seven assets, JCT ID,
name and symbol match; quote time missing; cap/circulating supply invalid and
total supply absent) and a Routescan transfer response with four rows. A
separate fixed-block log request matched one of those four; it cannot establish
the other three. Blockscout and Ethplorer holder calls failed with
`ConnectException`. Four minutes later Routescan transfer/log requests also
failed, while CoinLore remained reachable. Reproducible reports and raw body
hashes are retained under `evidence/acquisition/20261022T020500Z-recheck/` and
`20261022T020800Z-core-recheck/`. This establishes intermittent request outcomes
on this host, not a stable feed. No source is enabled; terms and external-use
rights remain unverified or restricted.

## Continuation verification (2026-10-22)

- Latest local test run: Maven 75 total (0 failures/errors, 1 opt-in acquisition
  probe skipped), Vitest 17/17, production frontend build passed, and Playwright
  14/14 across desktop/mobile.
- `scripts/verify-local-restart.ps1` preserved the isolated MySQL workspace and
  table preference digest across application restart; see
  `evidence/local-restart-verification-20261022023608658.json`. MySQL remained
  running, so database crash and production restore remain untested.
- Runtime smoke check confirmed the seven-entry catalog includes the verified
  Ethereum JCT identity, but JCT quote, derivatives, holders and transfers are
  unavailable under the current source gates. No production-approved sources
  were observed. Local missing-state acceptance does not satisfy the live-data
  acceptance requirement.
- An attempted Maven package while the running preview held the artifact
  produced an incomplete plain JAR and broke static-file responses. Both app
  processes were stopped, the executable package rebuilt successfully, and
  8080/8081 were restarted. Root and JS asset GETs returned HTTP 200 afterward;
  the subsequent full Playwright rerun passed. This records the verification
  interruption and recovery; it does not change source or production status.
