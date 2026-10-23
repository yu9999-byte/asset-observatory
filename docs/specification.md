# Asset Observatory specification v1

Status: implementation contract; external deployment and data redistribution are gated.
Date: 2026-10-21. Owner: current project user. No financial operations.

## Authoritative scope correction (v2)

The 2026-10-21 interface realignment contract is `docs/ui-realignment.md` (UI01-UI12).
It restores the compact three-page structure from the user's supplied Goal.
Disabled source-dependent filters and explicit missing columns remain incomplete
feature acceptance, not a reduction of the original backlog.

The user's supplied prior-discussion screenshot is part of the acceptance requirements. The initial
six-symbol catalog is a connectivity test set, NOT a replacement for the complete scope. MySQL is
the explicitly requested database and supersedes the earlier embedded-H2 architecture decision.
JCT is identified as Janction through its official tokenomics and the public Arkham token page;
the Ethereum validation contract and separate BNB deployment are recorded in
`docs/evidence/jct-identity-verification.json`. Independent Blockscout/Ethplorer
metadata confirms 18 decimals for Ethereum. Add this explicit deployment to the
catalog as JCT, with null spot/swap mappings until venue identity is verified.
No ticker-based venue mapping is inferred. The UI can navigate JCT even when
sources are disabled; that is missing-source navigation, not live-flow acceptance.
API entitlement, venue mappings and full JCT flow acceptance remain open.
CoinAnk is a visual reference (white surface, fine rules, dense tables,
signed red/green changes and proportion bars). Arkham is a reference for provenance-backed address
labels, quantity/value changes and flows, not permission to scrape or duplicate its private data.

### Full original acceptance backlog

| ID | Required feature / semantics | Current release gate |
| --- | --- | --- |
| R13 | Markets: circulating market cap, FDV, funding rate and price changes | verified supply/price/funding sources and definitions |
| R14 | Below-market rankings: price change, unusual volume, OI change, liquidation | comparable history, windows, scope and completeness |
| R15 | Holders: chain, balance, proportion, USD value; Top30/multi-address/custom-address filters | verified index, denominator and price coverage |
| R16 | Holder change horizons: 1H,24H,3D,7D,1M,1Y | quantity history separate from valuation history; unavailable backfill explicitly missing |
| R17 | Transfers: monitor address, From, To, event time, quantity, historical event price and USD amount | never substitute current price for historical event price |
| R18 | Transfer filters: inbound/outbound/exchange counterparty | labels and exchange attribution require documented evidence |
| R19 | Address statistics 24H/3D/7D: cumulative in/out/net and same-period market OI change | on-chain address holdings never conflated with venue OI |
| R20 | Per-user account permissions and private watches/notes | database ownership enforced on every read/write; tests with two users |
| R21 | Persist user records and required, permitted market/holder history and checkpoints in MySQL | first verify acquisition; persist only required authorized history, with retention and bounded ingestion |
| R22 | Retry/backoff, gap detection/backfill, event deduplication, chain reorg handling | cursor/event/block identity and finality evidence |
| R23 | Recover table layout/preferences after reload | per-user preference persistence and E2E |
| R24 | CI, container deploy, backup/restore, rollback and failure drill | actual runtime evidence; container config alone does not pass |

Proportion = holder quantity / explicitly selected denominator (total supply or circulating supply),
never silently switch denominators. Multi-signature/entity/project/exchange labels remain unknown
unless attributable evidence exists. Asset identity = canonical asset mapping + chain + contract;
matching ticker text alone is prohibited. USD valuations require quote conversion evidence.

All R01-R24 remain in the completion contract. Marking a source-dependent feature unavailable is
honest behavior, not acceptance of that feature. The release remains incomplete until those gates
are met or the user explicitly revises scope.

### Acquisition-first correction (2026-10-22)

The user clarified that MySQL is a storage choice when persistence is needed, not
the acquisition mechanism or a mandatory stage for all public market responses.
DA01-DA08 in `data-acquisition.md` govern the source investigation. Current Java
evidence and field-level availability are in `evidence/data-acquisition-2026-10-22.md`.
First verify transport, identity, metrics, timestamps, completeness and applicable
rights; then select a live query, bounded cache or approved MySQL history. A probe
sample is never a production fallback. Neither four historical transfers nor a
seven-symbol directory proves current holders, real-time quotes or full coverage.

R09 adds optional dated source-review metadata to `/api/sources`; transport
verification, activation and production approval remain three independent states.
This exposes acquisition progress without presenting unapproved market samples.

### R23 table preference contract (2026-10-21 increment)

Persist column visibility, column order, sort key/direction and page size in MySQL,
scoped by server-authenticated owner and stable table ID. The five initial IDs are
`markets`, `holders`, `transfers`, `liquidations`, `watches`. Holders/transfers share
their table-type settings across assets/addresses; labels are not storage keys.
Search text and current page are transient and are not restored. Allowed page sizes
are 15, 30 and 60; upstream cursor pagination still describes only loaded rows.
At least one column stays visible. Unknown columns, duplicate order entries and
incomplete column orders are rejected. Default order follows the documented table
schema. New settings have version 0; writes compare and increment the version.
Conflicting tabs receive 409 and must reload settings before another write. Failed
loads/saves are visible and never presented as persisted. Restore defaults is a
versioned write. No browser-local storage substitutes for MySQL persistence.

### R08 query scope and global-result gate (2026-10-22)

`docs/table-query-contract.md` is the SDD source for shared-table query scope.
The fixed seven-asset catalog is not a complete market universe. Holders and
transfers expose one provider cursor page at a time; client-side search, sorting,
counts and exports apply only to supplied rows. Their upstream total, stable
snapshot and complete ordering are unknown. Liquidations remain a bounded sample.
Therefore no page-local result may be labeled as a complete ranking or period
aggregate. Null sorts last in both directions; decimal comparisons remain exact.

Global table acceptance requires a bounded server query over a provably complete
declared scope (or an authorized, continuously maintained MySQL snapshot), with
allow-listed filtering/sort keys, deterministic null ordering and unique tie
breakers, query-bound snapshot cursors, nullable totals and provenance/completeness
metadata. The current API does not implement that contract. R08 global sorting
remains open until the holder/transfer source and retention gates are met; see
the query contract for acceptance tests and exact unlock conditions.

## Outcome and scope

Build a Java service and one Chinese, light, minimal research interface with these routes:
`/` markets -> `/assets/{id}` holders -> `/addresses/{address}?asset={id}` transfers.
Catalog: BTC, ETH, LINK, UNI, USDC, WBTC and verified Ethereum JCT. A catalog entry
is not a claim of source coverage. Instrument IDs are nullable when unverified;
adapters skip these assets rather than inventing an instrument from the symbol.
Ethereum mainnet ERC-20 contracts are explicit catalog configuration. BTC has no Ethereum holder
coverage; ETH native holders are outside the initial ERC-20 index scope and must show unsupported.
Never imply global market, all-chain, exchange entity ownership, or complete liquidation coverage.

## Requirements and verification

| ID | Requirement | Verification surface |
| --- | --- | --- |
| R01 | Market price, rolling 24h change and quote turnover, source and timestamps | adapter tests; live comparison report; market UI |
| R02 | ERC-20 holder quantities with raw integer/decimals, source pagination | precision and cursor tests; live holder reconciliation |
| R03 | Address transfers: hash, log identity, direction, token amount, time and block | fixture mapping; live transfer reconciliation; navigation E2E |
| R04 | Exchange perpetual open interest, unit and venue | metric tests and documented provider fields |
| R05 | Liquidation event samples with explicit bounded coverage, never fake 24h totals | missing-data tests; UI limitation text; live probe |
| R06 | Watch assets and addresses; server persisted; remove supported | HTTP integration and restart/restore drill |
| R07 | Floating notes by entity; save, reopen, show/hide, bounded length | persistence, validation, XSS and browser tests |
| R08 | Reusable tables: filter, sort, pagination, column visibility, copy/export | frontend tests and keyboard/browser acceptance |
| R09 | Every response carries status, source, fetchedAt, sourceAt, coverage and missing reason | API contract and failure tests |
| R10 | Bounded requests, caching, stale retention, timeouts, backoff; no invented zeros | deterministic cache/failure tests |
| R11 | Safe loopback default; external mode needs credentials, TLS proxy and source-use review | startup validation, security tests, deployment gate |
| R12 | Health/readiness, structured logs, backups, restore and rollback runbook | local deployment/restore evidence; container drill if runtime available |

R07/R20 project-record increment: `project-records.md` PR01–PR08 defines separate
user-authored project name, project information and custom note fields, all
persisted by entity/owner with one optimistic version. Hover previews saved
fields; the compact floating editor supports save, hide, reopen, pin and fold.
These records never become asserted project/entity labels or source content.

## Permission and deployment model

Default: bind 127.0.0.1; unauthenticated local access uses the `local-workspace` owner.
Authenticated workspace access uses the server-authenticated username as owner; clients cannot select
another owner. Watches, notes, limits and optimistic versions are isolated by owner. Existing v1 data
migrates to `local-workspace`. No exchange API keys, wallet keys, trade endpoints, signing, deposits or withdrawals.
External mode: operator credentials required, CSRF protection for changes, HTTPS terminating reverse
proxy, private network firewall. Account provisioning currently supports one operator; a managed
multi-account lifecycle remains a release gap even after database owner isolation passes tests.
Never publish or open firewall automatically.
Unreviewed redistribution terms prevent public distribution and must appear as a release gap.

## Time and performance contract

Internal ISO-8601 UTC timestamps; browser display Asia/Shanghai with timezone label.
The history model, checkpoint transaction and gap/reorg rules for R21-R22 are
specified in `docs/data-model.md`. Workspace V1-V3 and the policy-gated holder
page journal V4-V5 are implemented; no production policy is seeded. Market and
transfer history, workers, cleanup, gap/backfill and reorg processing remain open.
Market polling target 30s; stale after 90s since last successful fetch. Holder/transfer cache TTL 60s,
stale after 180s. These are application freshness targets, not source SLA. Missing source timestamps
remain null. Provider outage is a first-class status; last known values retain their original time.
Warm local API p95 < 300ms at 20 concurrent readers for 60s; upstream requests timeout in <= 10s
each and total foreground source retrieval <= 20s. Browser first usable view target <= 3s on localhost
after build (report measured hardware/runtime). Cold upstream work is reported separately.
Initial limits: <=100 rows per upstream page; 20 watch entries per type per workspace; notes <=4000
characters; fixed catalog; no arbitrary URL requests; bounded cache keys and source concurrency.

### Local runtime evidence protocol

Use the isolated `observatory_test` instance for verification. A restart drill reads
a nonempty watch/note workspace and saved market layout, stops only the verified
project Java process, restarts the same database/loopback port and compares the
canonical response SHA256 and versions. MySQL stays running; this is application
restart evidence, not database crash recovery or production recovery.

The warm API measurement uses 20 closed-loop readers for at least 60 seconds,
cycling markets, holders, transfers, liquidations, workspace and table preferences.
Warm every route first, report per-route and overall p50/p95/p99, errors, request
counts, duration, hardware and runtime. Validate JSON shape and explicit missing
status instead of treating every HTTP 200 as success. Each route must have p95
below 300ms with zero HTTP, transport or shape errors. Sources-disabled results
establish local response/persistence cost only; enabled-source cache, upstream
retrieval budgets, browser startup and production capacity remain separate gates.

### R10 enabled-source request budget

Allow at most two active upstream requests across providers and one start per
second. Saturated callers wait at most 100ms for a slot, then receive an explicit
unavailable result (or the original stale snapshot). Each admitted request has a
10-second total wall-clock budget, including pacing, headers, full response body
and parsing; cancellation releases its slot. Reject redirects, non-HTTPS/default
port URLs, credentials, fragments and non-allowlisted hosts. Cap response bodies
at 4,000,000 bytes while streaming, before accumulating larger payloads.

Cache single-flight is per key. A slow address must not lock unrelated addresses.
Bound concurrent cache misses by cache capacity and bound follower wait to 10s;
on timeout preserve any prior successful snapshot and original fetchedAt. Retry
after the dataset TTL; no immediate retry storm. Deterministic transport/cache
tests establish these software bounds without invoking or fabricating a provider.

## SDD -> TDD execution

The versioned OpenAPI 3.1 contract covers every registered business API and the
health routes. Maven contract tests compare the business route/method inventory,
validate real MVC responses against JSON Schema 2020-12 using a standard validator,
and verify that missing provenance, numeric decimal quantities and invalid request
fields are rejected. Nonempty test records are synthetic software fixtures only.
Local anonymous access and external HTTP Basic access are separate modes; write
operations additionally need the session cookie and CSRF header. Security errors
have no promised application-error JSON body. No fixture establishes source coverage.

1. Record source probes, conditions, metric definitions and exact API contract before adapters.
2. Write failing unit/integration tests for precision, absent/stale data, persistence and security.
3. Implement minimal passing adapters/services; never wire fixtures into production.
4. Implement complete UI flows and browser acceptance; attach traceability evidence per requirement.
5. Reconcile real source samples, load-test warm APIs, restart and restore saved state; document gaps.

## Iteration and honest stop

For every blocked source: retain URL, UTC, HTTP/transport result and response hash; verify an official
or documented alternative. No geo-restriction bypass, login scraping, undocumented paid access or
assumed redistribution grants. Keep unsupported data null with reason. When a required source or
deployment prerequisite cannot be met, continue independent implementation and report exact unlock
condition. Passing tests with fixtures proves software behavior, not live coverage or production readiness.
Completion requires all applicable R01-R24 evidence, no required unverified data permission, and an
actual deployment recovery drill; absent evidence leaves the Goal active and release unapproved.
