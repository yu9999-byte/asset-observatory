# Table query and completeness contract

Status: SDD contract for R08. The configured seven-asset market directory now
has a complete query endpoint; provider-backed holder and transfer pages remain
partial.
Updated: 2026-10-22.

## Current observable scope

| Table | Current input set | Filter / sort / page meaning | Completeness claim |
| --- | --- | --- | --- |
| Markets | `GET /api/markets/query` snapshots the fixed seven-asset catalog and joins source measurements when available | Server-side `q`, allow-listed sort/direction, bounded `limit`, and opaque snapshot cursor; null measurements sort last in either direction | Complete for the configured seven-asset directory, never the market universe; each metric retains its own source coverage |
| Holders | One upstream cursor page per request | Table controls see only the currently loaded provider page; provider pager moves to another page | Partial; total count, stable snapshot, complete ranking and cross-page order are unknown |
| Transfers | One upstream cursor page for one address and asset | Direction filter and table controls see only the currently loaded provider page | Partial; total count, stable snapshot, complete period aggregates and historical price coverage are unknown |
| Liquidations | The bounded provider response described by its envelope | Table controls see only returned sample rows | Partial bounded sample; not a complete 24-hour or market-wide result |
| Watches / notes | Current authenticated workspace response | Client controls see all rows returned by that owner-scoped response | Complete for the returned workspace response, subject to its explicit server limits |

The shared table must not turn an incomplete source page into a complete dataset.
Its current search, sort, export, row count and page count describe the rows passed
to the component, not an upstream total. For provider-backed tables, the UI must
keep source pagination and its coverage text visible and must not label a page-local
rank as Top N. Missing values remain last in ascending and descending sort order.

## Market directory implementation

`GET /api/markets/query` is the first endpoint that satisfies the contract for a
declared complete scope. It returns `total`, `completeness=complete`, a UTC
`snapshotAt`, and a cursor bound to `q`, `sort`, `direction`, `limit`, and a
two-minute MySQL snapshot containing the ordered rows and source provenance. It is deliberately separate from the composite
`GET /api/markets` response so the existing three-page UI contract remains
backwards compatible. A disabled or unavailable measurement source produces
catalog rows with explicit null metric fields and a reason; it does not reduce
directory completeness or turn missing metrics into zero.

The markets table now calls this endpoint for search and persisted sorting; its
page-size preference is forwarded as `limit`. The displayed metrics still join
the separate per-source derivative envelope, and the directory's query
completeness does not imply measurement completeness. Snapshot tokens are
snapshot state is persisted in MySQL, so application restarts and load balancing
across instances do not invalidate continuation. Expired rows are removed during
new snapshot creation; storage is capped at 32 snapshots by deleting the oldest
rows. This cursor persistence does not establish database crash recovery or
production database availability.

## Required query contract before global table acceptance

R08 global-result acceptance is blocked until a table has a queryable, bounded,
complete result set or a provider proves equivalent semantics. Each eligible
endpoint must define:

1. `scope`: canonical asset/chain/contract/address and any venue or time window;
   the scope is part of the cursor and cannot change between pages.
2. `filter`: allow-listed fields and operators, normalized values, case and address
   rules, and whether filtering is server-side over the entire declared scope.
3. `sort`: allow-listed fields, explicit numeric/string/time type, direction,
   deterministic null placement (null last in either direction), and a unique
   stable tie-breaker. Decimal values are compared without binary floating point.
4. `page`: bounded page size and opaque cursor bound to scope, filter, sort,
   direction, tie-breaker and snapshot/version. Cursor reuse under changed query
   inputs is rejected; repeated or expired cursors return a documented error or
   explicit restart instruction.
5. `completeness`: `complete`, `partial` or `unknown`; exact total only when the
   source can prove it. Unknown total is `null`, never inferred from page length.
   Include source observation time, coverage window, freshness, and missing reason.
6. `authorization`: source-use and retention policy gate, plus authenticated
   owner scoping for persisted user data. Public market queries never accept a
   client-supplied owner identifier.

The preferred application API shape is `GET /api/{table}` with `scope`,
`filter`, `sort`, `direction`, `limit` and opaque `cursor`; its response contains
`data`, `nextCursor`, nullable `total`, `completeness`, `snapshotAt`, and the
existing per-source provenance. This shape is a design target only. Do not add
or advertise an operation until its data source can uphold the query semantics
and the corresponding OpenAPI schema and contract tests are approved.

For MySQL-backed datasets, server-side filtering, ordering and pagination must
operate over the declared persisted dataset in one consistent snapshot. The
query plan needs bounded indexed predicates and a unique final order key. MySQL
is not a substitute for missing upstream coverage: an incomplete ingestion
remains `partial`, and out-of-window or unobserved values remain absent.

## Acceptance tests

- A fixture spanning multiple source pages places matches and sort extrema on
  pages that are not the first page; the endpoint returns the globally correct
  filtered order without duplicates or omissions for one snapshot.
- Equal sort values use the documented unique tie-breaker; nulls remain last in
  both directions; large integer/decimal values retain exact order.
- Changing scope, filter, sort, direction or snapshot invalidates the old cursor.
- Unknown totals stay null. Partial or expired coverage is exposed and cannot be
  rendered as a complete Top N or complete-period aggregate.
- Two authenticated users cannot query each other's persisted watches, notes,
  preferences or any future owner-scoped saved dataset.
- UI row counts, CSV exports and page controls state whether they cover the full
  declared result or only loaded rows.

## Current unlock conditions

Holder sorting requires an eligible source with complete declared coverage or an
authorized and continuously maintained MySQL snapshot, plus stable snapshot
pagination and independent reconciliation. Transfer sorting/statistics require
the same for a declared address/asset/window, with event identity, reorg/finality
policy and historical prices separately verified. Until these gates pass, current
provider-page sorting remains explicitly page-local and R08 global-result
acceptance stays open. See `evidence/data-acquisition-2026-10-22.md` and G01-G06.
