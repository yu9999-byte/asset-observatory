# Execution plan — approved scope, updated 2026-10-22

User authorization: execute the agreed plan and the later explicit request to
assist with free-provider account registration. No public publishing, spending,
paid upgrade, messaging, or financial operation is authorized.

| Stage | Deliverable / acceptance | Current state / dependency |
| --- | --- | --- |
| 1. Acquire before architecture | Java probes, raw SHA256, identity, precision, coverage and failure alternatives (DA01–DA07) | Bounded quote/transfer evidence obtained; holder/derivative/external-use gates explicitly open; see evidence/data-acquisition-2026-10-22.md |
| 2. Freeze verified coverage | Source matrix, field dictionary, refresh/permission/cost/retention and explicit missing values | Matrix updated with the 2026-10-21 chain/market verification reports; no eligible complete external data contract yet; G01–G05 block full-flow freeze |
| 3. SDD contracts | R01–R24/UI01–UI12 traceability, OpenAPI, metrics, ownership and MySQL data model | Baseline contracts exist; R09 source review and PR01–PR08 private project fields added; R08 market directory query is implemented, while holder/transfer global queries and history APIs remain gated |
| 4. TDD source implementation | Adapter failures before implementation; precision/identity/expiry/policy tests; no production fixture fallback | Mapping tests and dated review UI implemented; runtime adapters wait for entitlement, identity and coverage evidence |
| 5. User research workflow | Markets → holders → transfers; shared tables, user watches, project records and floating notes | MySQL watch/note/preferences and separate project name/info/custom-note fields verified; verified live data, rankings, global holder/transfer sorting and managed accounts still open |
| 6. Continuous ingestion | Bounded scheduler, checkpoint, retry/backfill, dedup, reorg, cleanup and historical coverage | Policy-gated holder journal only; workers/history sources and full replay pending G02–G05 |
| 7. Integrated acceptance | Real-provider → adapter → MySQL/cache → API → desktop/mobile; independent reconciliation | Local missing-state E2E and sample matching only; entitled enabled-source acceptance pending |
| 8. Production evidence | Enabled-source load/outage, CI, deployment/rollback/backup/restore, actual environment | Prior local MySQL restore/restart/load evidence exists; production and enabled-source drills pending G06 |

Do not count missing-state UI or passing synthetic tests as full feature
acceptance. Every runnable increment updates traceability with the actual test
result. A blocker report identifies evidence, affected features and the exact
unlock; it never silently enables an unreviewed source or fabricates completion.

Latest local rerun (2026-10-22): frontend unit tests 16/16, Java tests 59/59
(one opt-in live probe skipped), production build, and Playwright 12/12 on
desktop/mobile all pass. This does not close the source entitlement, complete
coverage, global query, continuous ingestion, or production recovery gates.

## Active continuation — 2026-10-22

The immediate critical path is access to a provider whose actual terms and
coverage allow public-user display. Account creation alone is not an acceptance
result. Work proceeds in this order:

1. **CoinAnk account and trial request:** user supplied a registration email.
   The official form requires email, one-time code, and a user-set password;
   invitation code is optional. Two bounded browser interactions did not
   produce a verification-send request or visible confirmation. No account,
   key, trial, or payment method exists. Resume only after the send action can
   be completed in the official page; then request a trial that names the
   public-user product, required fields, limits, and display/storage rights.
2. **Independent public-source route:** keep checking official, keyless routes
   already sampled and any bounded alternatives, but do not activate them until
   external-use terms and deployment-host reachability are established. Existing
   evidence shows sample quotes/transfers only; it does not provide complete
   holders, full market coverage, or allowed redistribution.
3. **Source-to-product acceptance:** once a provider is eligible, capture its
   live catalog and exact JCT observations, map contract/instrument identity,
   reconcile timestamps, units and coverage against CoinAnk/Arkham, then write
   failing tests before enabling each adapter. Missing fields remain explicit.
4. **Full-flow acceptance:** verify real provider data through Java adapter,
   MySQL/cache, API and the existing pages; then run enabled-source outage,
   recovery, load, deployment and backup-restore drills in an available target
   environment. Local missing-state tests do not close this stage.

Current checkpoint: `/api/sources` on local port 8080 is reachable and reports
all configured providers disabled and not production approved. `/api/health`
did not respond within 10 seconds in this check. CoinAnk registration is at the
email-code step; no code request was observed. Source and deployment gates
remain open; no live-data or production acceptance is claimed.

## Market query integration rerun (2026-10-22)

The markets table now uses `GET /api/markets/query` for its search text, saved
sort key/direction and page-size preference. The page renders only the server
returned directory rows, while derivative fields continue to come from their
separate envelope. A new R08 browser case verifies request parameters and the
JCT result on desktop and mobile. The query remains complete only for the
configured seven-asset directory; its two-minute cursor snapshot is stored in
MySQL with a 32-row retention cap, so pagination is independent of application
process restarts and instance routing. Production database availability and
crash recovery remain deployment gates.

## Deployment contract increment (2026-10-22)

- Added a multi-stage `Dockerfile`, MySQL 8.4 Compose service, external-mode
  safety defaults, CI workflow, and password-safe PowerShell backup/restore
  scripts. The deployment SDD is `docs/deployment.md`.
- Static verification passed for both scripts, Maven packaging, and the 16
  frontend tests. Evidence is
  `evidence/deployment-contract-verification-20261022.json`.
- Docker Compose parsing, image build, database crash/restart, and rollback were
  not run because Docker is not installed on this host. The deployment gate is
  therefore still open; configuration files are not counted as runtime evidence.

## Source verification increment (2026-10-22)

The independent chain and market verification reports are wired into runtime
source-review metadata. `/api/sources` continues to report transport, rights and
production approval as separate states. Browser or proxy reachability does not
enable a provider: Blockscout remains unavailable to the Java host, Kraken
remains local-use only with unverified JCT instrument identity/OI unit, and
Routescan remains a bounded sample with unresolved persistence and redistribution
terms. No new adapter or fixture was enabled.

## Transfer ingress and source review increment (2026-10-22)

TX01-TX07 now cover exact event identity/quantity/time parsing and conflicting
duplicate rejection with recorded failing-then-passing tests. DA10 now exposes
the official terms review independently of quote observation time and provider
activation; unknown terms URLs remain null. See `transfer-ingress.md`,
`data-acquisition.md` and `traceability.md` for requirement-to-test links.

CoinLore's official terms contain an express-written-consent restriction on
redistribution/commercial use. Its API samples do not establish an external-use
entitlement. The corrected MEXC contract endpoint timed out in Java and curl;
no JCT instrument or usage rights were verified. Details and exact unlocks are
in `evidence/source-documents-review-2026-10-22.md`. All sources stay disabled.

Latest increment verification: Maven 71 total / 70 passed / one opt-in probe
skipped / zero failures or errors; frontend 17/17; TypeScript/Vite production
build and Maven package passed. The two targeted source-review browser cases
passed on desktop/mobile; the full earlier browser suite was not rerun in this
increment. Both previews were restarted from the rebuilt package and the 8080
health/source metadata verified. Evidence is retained in
`evidence/transfer-source-increment-20261022.md`.

Next dependency gates remain: eligible external display/storage terms; current
complete holder coverage and JCT derivatives mapping; event-price/history
coverage; continuous ingestion and global holder/transfer queries; managed
external accounts; and a runnable production/container environment for actual
deployment, rollback and database recovery drills. No source, feature or
production gate is closed solely by passing offline or missing-state UI tests.

## Holder observation retention increment (2026-10-22)

Flyway V8 adds aggregate cleanup audit records. A scheduled task removes expired
holder observations according to each source policy, bounded to whole batches
and at most 1,000 rows per run. MySQL integration tests cover retention policy,
disabled/expired sources, bounds, fresh observations and rollback. Full Maven
verification passed 75 tests (one opt-in live probe skipped, zero failures or
errors); Flyway reported schema version 8 in the isolated test database. Docker
runtime, production scheduling and recovery are still unverified. The rebuilt
JAR is running at `127.0.0.1:8080` and `:8081`; both databases migrated to V8,
both health checks are UP, and all six source records remain disabled and not
production approved.

## Continuation verification and CoinAnk discovery (2026-10-22)

- Current rerun: Maven `test` passed 75 tests (0 failures/errors; 1 opt-in live
  acquisition probe skipped) against MySQL 8.4, Flyway schema V8. Frontend
  Vitest passed 17/17 and `npm run build` passed TypeScript/Vite.
- Both existing loopback app instances returned health `UP` on ports 8080 and
  8081. This was a health check only; no source was enabled and no full browser
  browser E2E, deployment or recovery drill had yet been rerun at that point.
- The full Playwright suite was then rerun against the isolated test database:
  14/14 passed across desktop and mobile, including source-review state, JCT
  three-page navigation, MySQL project records/watches, layout conflict handling
  and server-side market query behavior. It remains missing-state/local software
  evidence, not enabled-source acceptance.
- Read-only CoinAnk page review found a rendered six-venue JCT derivative table,
  while its API route rendered empty and its terms did not grant external
  redistribution. Evidence is in `evidence/coinank-jct-page-review-2026-10-22.md`;
  no adapter or runtime value was added.
- The current host has no Docker CLI and `agent-reach` is not installed. Docker
  image/compose/recovery acceptance and agent-reach source retrieval are
  therefore still open. The web-access CDP bridge was used in a separate
  temporary tab, which was closed; the user's existing tabs were untouched.

No full-flow acceptance is claimed. G01-G06 remain open, including authorized
external data, complete holders/transfers, JCT derivatives methodology, event
price/history, continuous ingestion, managed external accounts and production
deployment/recovery.

## Current host readiness check (2026-10-22)

The active execution environment returned HTTP 200 on both local preview health
endpoints. `/api/sources` returned six source records, with zero enabled and
zero production approved. This confirms only that the missing-source application
state is running. The current PowerShell environment cannot resolve Java,
`javac`, or Maven through `PATH`; the project-pinned JDK 21 and Maven 3.9.11 are
available under `.tools`. With those paths configured, a new three-endpoint
Java holder-source probe ran and each endpoint returned a host-side
`ConnectException` without HTTP status/body. Evidence and exact reproduction are
in `evidence/holder-source-recheck-2026-10-22.md`. `agent-reach` and Docker remain
unavailable here. Do not interpret the transport failures as empty holder data
or as provider-side outages; no deployment drill was performed.

## Continuation verification (2026-10-22)

- Re-ran Maven tests against MySQL 8.4: 75 total, zero failures/errors, one
  opt-in live acquisition probe skipped. Frontend Vitest passed 17/17 and the
  TypeScript/Vite production build passed.
- Playwright passed 14/14 across desktop and mobile after rebuilding and
  restarting both local previews. The JCT flow, MySQL-backed notes/watches,
  table preferences, and explicit unavailable-source states all passed.
- Restarted the isolated `observatory_test` application after the browser
  writes. The canonical workspace/layout SHA-256 remained
  `15F2A17BB324F283DA3DAEB3ED3AA3BF6D9F899C0BEF8E302FACD4FC797FB683`;
  evidence is `evidence/local-restart-verification-20261022023608658.json`.
  This covers an application restart with MySQL running, not database recovery.
- Live local API inspection returned seven configured catalog entries, one JCT
  directory row, and no quote, derivative, holder, or transfer measurements.
  `/api/markets` and the JCT holder/transfer envelopes report `unavailable`;
  this is the intentional source-policy gate, not a zero-value observation.
  The six configured source records remain disabled and not production approved.
- Docker remains unavailable, so image build, container deployment/rollback,
  database crash/restart and production recovery acceptance remain open. No
  source entitlement or full-flow acceptance gate is closed by these local tests.

## Public-source route recheck (2026-10-22)

After prior direct HTTPS timeouts, the verified Ethereum JCT CoinGecko contract
endpoint was opened once through the web-access CDP proxy in a new background
tab. It returned a Chrome network error page with no HTTP response or data. The
temporary tab was closed; existing user tabs were not touched. This is recorded
in `evidence/acquisition/coingecko-cdp-20261022/report.json` and does not prove
CoinGecko lacks JCT data. No source or rights gate changed. The workspace still
has zero production-approved market sources and cannot complete live-data
acceptance until a permitted, reachable source route and its external-use
conditions are established.

## Verification rerun after source-route check (2026-10-22)

- Maven tests against local MySQL 8.4 completed with exit code 0; 75 tests were
  discovered across 14 Surefire suites. The ordinary live acquisition probe is
  opt-in and skipped. Flyway schema V8 was current. Maven emitted its existing
  warning that this Flyway version officially tests MySQL only through 8.1.
- Frontend Vitest passed 17/17 and TypeScript/Vite production build passed.
- Playwright passed 14/14 across desktop and mobile against the isolated local
  preview, including three-page navigation, private record persistence,
  server-side market query behavior and explicit unavailable-source states.
- Port 8080 health returned `UP`; `/api/sources` returned six configured
  providers, all disabled and not production approved. The UI therefore has no
  market, holder, derivative or transfer measurements from production sources.
- Docker CLI is absent. Image build, container deployment, rollback, database
  crash/restart and production recovery were not run. External data rights,
  complete JCT holder scope, instrument methodology, event-time prices,
  historical continuity and managed external accounts remain acceptance gates.

This rerun validates local software behavior only. It does not close G01-G06,
prove external source availability/rights or satisfy live-data acceptance.
