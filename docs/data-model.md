# MySQL data and ingestion contract v1

Status: workspace tables V1-V3 and the internal holder-page journal V4-V5 are
implemented. The broader ingestion/history model below remains a specification
for R21-R22. No enabled production policy, worker or history acceptance is claimed.
All business persistence uses MySQL 8.4 LTS, Flyway, utf8mb4 and UTC. No source
data is stored until a documented policy explicitly allows that dataset, purpose,
storage duration and display audience. A reachable endpoint alone never enables it.

`market_query_snapshots` stores the complete configured-directory result used by
`GET /api/markets/query`: UUID cursor, normalized filter/sort/page inputs, UTC
snapshot time, ordered market rows JSON and the source provenance envelope JSON.
Rows expire after two minutes and are bounded to the newest 32 snapshots. This
supports continuation across application instances while leaving database
availability and crash recovery as deployment concerns.

## Implemented workspace model

| Table | Identity | Persisted state / concurrency |
| --- | --- | --- |
| watches | owner_id, kind, entity_key | creation UTC; 20 per kind per owner |
| notes | owner_id, kind, entity_key | project name/info, custom text, visibility, version, update UTC; conditional version update |
| table_preferences | owner_id, table_id | JSON column configuration, version, update UTC; conditional version update |

Owner is computed from the authenticated principal, never accepted from a client.
Existing anonymous records use local-workspace. These tables are private user
state; source observations are separately scoped shared research data and carry
their permitted audience. They cannot expose private watches or notes in history.
MySQL failure never returns a confirmed write. Current timestamps are ISO strings;
new ingestion timestamps use DATETIME(6) with UTC JDBC connections.

Project-record migration V6 adds project_name VARCHAR(120) and project_info
VARCHAR(4000), both NOT NULL DEFAULT ''. Existing body/owner/visibility/version
and time are unchanged; old inserts use defaults. Optional/null API fields keep
their stored value with COALESCE inside the version-conditional update; explicit
empty strings clear them. All values are private user input and rendered inert.
See `project-records.md` for UTF-16 limits and compatibility acceptance.

## History identities and precision

- Asset: verified canonical catalog mapping plus chain ID and contract bytes.
  Ticker text cannot identify an asset. JCT deployment identity is corroborated in
  `evidence/jct-identity-verification.json`; Ethereum 18 decimals and selected
  real fields are checked in `evidence/jct-sample-verification.json`. The catalog
  has null venue mappings. Holder discrepancies and source activation gates remain open.
- Venue series: provider + venue + instrument + metric definition version + unit.
  Different venues, units and window definitions never share a time series key.
- Raw ERC-20 quantities: unsigned integer strings, maximum 78 decimal digits
  with an explicit uint256 bound; MySQL DECIMAL's 65-digit limit is insufficient.
  Store canonical strings and calculate using Java BigInteger/BigDecimal.
- Price and derived quantities: canonical plain decimal strings, explicit unit,
  precision and rounding policy. JSON numeric doubles cannot hold these values.
- Timestamp: source_at is nullable, fetched_at is actual successful observation
  time, persisted_at is commit observation time. Never replace source time with
  fetch time or re-stamp cached/stale values as a new measurement.
- A holder page with no block/snapshot identity is an observation of that page,
  not a consistent full token balance snapshot. A final page is not proof that
  preceding pages describe the same block.

## Proposed entities

| Entity | Unique key / indexes | Required data |
| --- | --- | --- |
| source_policy | provider, dataset, policy_revision | evidence URL/hash/date, permitted audience, retention limit, approval expiry, enabled flag |
| ingest_stream | provider, dataset, canonical scope hash | version, policy revision, checkpoint JSON, observed head, complete-through watermark, retry state, next_attempt_at; due-work index |
| ingest_batch | stream_id, idempotency_key | fetch identity, policy revision, payload SHA256, source/fetch times, coverage, row count, checkpoint before/after; stream/time index |
| market_snapshot | series_id, observation identity | exact price/volume/turnover/OI strings, explicit units, window, null missing fields, batch reference; series/source-time and series/fetch-time indexes |
| holder_observation | batch_id, address | chain/contract, raw quantity, decimals, source block/hash if provided, page scope and coverage; asset/address/time index |
| transfer_occurrence | chain, contract, tx_hash, log_index, block_hash | block number, From/To bytes, raw quantity, decimals, event UTC, canonical/finality status, batch reference; address/block indexes on From and To |
| chain_block | chain, number, hash | parent hash, first/last observed times, canonical/finality status; canonical height index |
| ingest_gap | stream_id, gap identity | reason, known block/time interval, detected/resolved UTC, backfill cursor, attempts, status |

Hashes and addresses use byte identity or case-sensitive normalized columns.
Checkpoint JSON contains only dataset-specific approved fields, never arbitrary
URLs. A batch cannot reference a different stream or expired policy. Identical
content at different source observations may be valid; payload hash alone is not
a measurement identity. Conversely, retries of one observation cannot create
another measurement. Missing/error responses are operational attempts, not
successful batches with invented zero data.

## Atomic ingestion and bounded work

1. Select a due stream using a bounded lease/version. Check current policy,
   permitted audience and expiry before fetching. Do not hold a MySQL transaction
   open during network I/O. Use the R10 shared concurrency and request budget.
2. Validate schema, asset identity, units, page cursor, row count, body size and
   timestamp semantics. Retain allowed raw evidence only within policy bounds.
3. In one MySQL transaction, lock/check the stream version and lease, insert the
   idempotent batch/observations, record detected gaps and advance only the proven
   checkpoint. Commit all or none. A lost lease must not advance a checkpoint.
4. A retry after crash uses the same observation/idempotency identity. Duplicate
   data does not advance version twice. A checkpoint can never point beyond
   committed observations. Restore/replay starts at a committed checkpoint.
5. At most 100 rows per page; at most one active worker per stream and two active
   upstream requests globally. A work cycle handles at most one page per stream
   before yielding to other due streams. An oversized result is rejected.
6. Retry retryable failures with full jitter and exponential ceilings of 2, 4, 8,
   16, 32 and 60 seconds, retaining the last successful timestamps. Respect a valid
   Retry-After up to 15 minutes. Terminal schema, identity and permission failures
   disable that stream pending review. No immediate infinite retry or hidden gap.

## Gaps, finality and history semantics

Polling an address/indexer page cannot establish continuous block coverage.
The complete-through watermark remains unknown until the source proves a
contiguous interval. Gaps may be unknown-width and are shown as incomplete;
backfilling observed pages does not silently resolve an unprovable gap.

Reorg support requires block hash, parent hash and an attributable canonical
chain/finality source. Existing transfer DTOs lack these fields, so they cannot
yet populate a reorg-safe history. Retain each block occurrence; never overwrite
an orphaned event as though it had not existed. A parent mismatch opens a gap,
rewinds to the verified common ancestor and atomically invalidates affected
canonical aggregates before replay. Search at most 128 blocks automatically;
a deeper/no-common-ancestor event halts the stream for a rescan. Do not label a
fixed number of confirmations as Ethereum finality. Unknown finality stays unknown.

Holder change uses the same address/asset quantity observations, with baseline
time and coverage shown. 1H/24H/3D/7D/1M/1Y use elapsed UTC windows (1M=30 days,
1Y=365 days). Select the closest valid observation at or before the baseline,
with maximum age 5 minutes for <=24H and 1 hour for longer windows. Missing
baseline, reorg or unresolved interval gap yields null with a reason. These
windows are unavailable until enough genuine history exists; never synthesize
past balances from today's balance. USD change and quantity change are separate.

Transfer valuation joins an eligible historical price at/before event time,
with maximum lag 60 seconds and explicit quote/USD conversion evidence. No
eligible price means null USD value. Current price cannot fill that gap.
Address IN/OUT sums exclude SELF from both; net=IN-OUT. A sum over incomplete
events is labeled partial and cannot feed a complete-ranking claim. Market OI
change remains a separate venue series with its own baseline and coverage.

## Retention and acceptance gates

Proposed upper bounds: raw evidence 7 days, normalized source observations
400 days, operational attempts 30 days. Actual limits are the stricter of these
bounds and the approved source policy. Cleanup is bounded to 1000 rows per
transaction, preserves unresolved reorg/gap references and writes cleanup audit
counts. Policy expiry stops collection/display as required by that policy;
retention is never assumed to be perpetual. Estimated storage and cleanup load
must be measured before enabling more than the six sample catalog entries.

Before implementation, add failing MySQL tests for atomic checkpoint/data
rollback, idempotent retry, concurrent lost-lease rejection, uint256 precision,
unknown timestamps, policy denial/expiry, bounded retention, page gaps and reorg
invalidation. Then prove actual source sample reconciliation and crash/replay
recovery. No history HTTP route or UI chart is claimed by this specification.

## First storage increment: holder page journal

This increment implements only policy-gated holder page commits. No production
policy is seeded, no scheduler is enabled and no public history API is added.
Market/transfer history, retention cleanup, retry scheduling and reorg processing
remain subsequent increments. It cannot establish continuous history or Top30.

An internal caller selects an existing catalog ERC-20 identity and policy revision.
Policies must explicitly permit `holders` storage for the `personal` audience,
include an HTTPS evidence URL and SHA256, expire in the future and specify a
retention limit of 1-400 days. A policy is checked again under a database lock
when committing; disabling/expiring it between fetch and commit rejects the page.
There is no administrative HTTP endpoint to grant permission.

The stream key is policy revision plus catalog chain/contract. Claiming a stream
locks its row and issues an unpredictable 30-second lease with an incremented
version. A commit requires that exact unexpired lease/version. One transaction
inserts a batch and up to 100 distinct address balances, records source/fetch
times and page coverage, advances an opaque checkpoint (maximum 1024 characters)
and releases the lease. It never advances a complete-through watermark.

The idempotency key (maximum 128 characters) identifies a successful observation.
Repeating exactly that committed observation returns its original batch without
advancing the stream again, including after a lease was released. Reusing its key
with different checkpoint, evidence hash, times, coverage or address balances is
an error. Page order does not change observation identity. Invalid uint256,
duplicate addresses, future fetch/source timestamps, source time after fetch,
unknown asset identity or oversized input rejects the whole page. Raw quantities
are canonical strings; unknown source timestamps remain SQL NULL. Transaction
rollback must leave batch, balances and stream checkpoint/version unchanged.

This journal is internal infrastructure. Test policies and balances are fixtures
in `observatory_test` only and never grant collection rights or source coverage.

## Holder journal retention cleanup (HC01-HC04)

`purgeExpiredObservations(maxRows)` removes only holder batches older than their
own policy's `retention_days`, measured from `persisted_at` in UTC. Disabled or
expired policies are still subject to cleanup. Each transaction selects a
deterministic oldest-first set of whole batches whose total observation count
does not exceed the requested bound (1-1000), deletes observations and batches,
and inserts one aggregate audit row. Parent batches are retained unless all
their observations fit the same bounded operation; empty batches are eligible
and count as zero observation rows. A single batch is never split. The caller
repeats bounded calls while work remains. Audit rows contain timestamps and
counts only, not addresses, balances, policy evidence, or event payloads.
The scheduled cleanup runs every 60 seconds after a 60-second startup delay and
deletes at most 1000 observation rows and their complete parent batches per run.

The current journal has no unresolved reorg/gap reference tables or readers;
cleanup does not claim reorg-safe history and is limited to this journal's
retention expiry. If future gap/reorg state references a batch, the eligibility
predicate must exclude that batch before cleanup is scheduled. The MySQL test
contract covers mixed policy retention, disabled policies, fresh-row retention,
the row bound, deterministic repeat calls and transaction rollback.

V5 fingerprints the policy's provider/dataset/revision, audience, evidence URL
and SHA256, expiry and retention at stream creation. Editing those fields forces
a newly reviewed revision/stream; toggling enabled can revoke or resume the same
unchanged revision. Each committed batch retains a structured JSON copy of that
policy evidence. Rows predating V5 keep NULL evidence/fingerprints and cannot be
silently accepted or backfilled with invented authorization. Times are normalized
to MySQL DATETIME(6) precision in UTC. Committing rechecks lease and policy expiry
after inserts and rolls back if either expired while persisting.

The isolated restore drill additionally seeds a disabled test-only policy and a
nonempty holder journal. It restores a consistent dump into a new database and
compares the full schema/data SHA256. Separate assertions check exact uint256
maximum/zero strings, NULL source time, UTC microseconds, committed checkpoint,
version, absent lease and immutable policy JSON. This proves dump/restore fidelity
for that fixture; it does not prove running-worker crash/replay recovery, real
source coverage or production recovery. Fixture hashes are synthetic and do not
establish provider evidence or application-generated observation identity.
