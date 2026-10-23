# HTTP contract v1 (frozen before implementation)

All API routes are same-origin, JSON UTF-8. Decimal quantities are strings. UTC timestamps use ISO 8601.
The machine-readable contract is `docs/openapi.json` (OpenAPI 3.1). Validate response schemas,
registered business operations and representative request/error cases during Maven tests.
Application-handled errors use `{code,message}`: invalid input 400, capacity/conflict 409,
storage failure 503. Unknown catalog IDs return 400; missing required query parameters use
VALIDATION_FAILED. Framework/security responses (including 401, CSRF 403 and unmatched routes)
do not guarantee this JSON shape.
An explicit JSON 413 handler is not implemented; do not rely on the servlet form-size limit
to bound JSON request bodies.
Write requests require an `X-CSRF-TOKEN` from `GET /api/csrf`, with the session cookie.
No CORS wildcard. In external mode the app shell, static assets, CSRF bootstrap,
and `POST /api/accounts` are anonymous. Registration is CSRF-protected and only
creates `RESEARCHER` accounts; every research/workspace/admin route requires
HTTP Basic authentication. Public internet exposure remains gated on abuse controls.

## Read routes

`GET /api/markets` => `{assets: Asset[], market: Envelope<Market[]>, derivatives: Envelope<Derivative[]>}`.
`GET /api/markets/query?q=&sort=&direction=&limit=&cursor=` => `MarketQueryPage`;
it filters, sorts and pages the configured seven-asset directory in one MySQL-
persisted snapshot containing the ordered rows and source envelope. Until this
directory is reconciled against the target market universe, `completeness` is
`partial`; `total` counts configured catalog entries only. Allowed sort keys are `assetId`, `instrument`, `lastPrice`,
`change24hPct`, `volume24hBase` and `turnover24hQuote`; missing measurements sort
last in both directions. The cursor expires after two minutes and
can continue across application restarts or instances that share the configured MySQL database. Expired rows are removed on snapshot creation; only the newest 32 snapshots are retained.
The markets table sends its search, saved sort and direction, and saved page size
to this endpoint; the other provider-backed tables retain page-local semantics.
`Asset`: `{id,symbol,name,contract:string|null,chain:string|null,spotInstrument:string|null,swapInstrument:string|null,decimals:number}`.
JCT denotes only verified Ethereum Janction, with 18 decimals; both venue
instrument mappings are null. A catalog entry does not imply a price or venue
listing. Missing mappings are skipped by venue adapters, never guessed by ticker.
`Market`: `{assetId,instrument,lastPrice,open24h,change24hPct,volume24hBase,turnover24hQuote,quote,sourceAt}`.
`Derivative`: `{assetId,instrument,openInterestContracts:null,openInterestUsd:null,openInterestRaw,openInterestUnit,sourceAt:null}`.
Kraken native OI unit remains unverified; raw values are explicitly marked and never labeled USD/base.
Kraken spot `o` is today's open, not rolling 24h open; open24h/change24hPct remain null. Quote turnover
is an estimate using published 24h VWAP times published 24h volume, not direct reported turnover.
`GET /api/assets/{id}/holders?cursor=...` => `Envelope<Holder[]>`.
`Holder`: `{address,balance,rawBalance,decimals,isContract}`.
This route returns one source page; it does not accept a global filter or sort,
does not report a total, and does not promise a stable cross-page snapshot.
`GET /api/addresses/{address}/transfers?asset={id}&cursor=...` => `Envelope<Transfer[]>`.
`Transfer`: `{id,hash,logIndex,blockNumber,from,to,direction,amount,rawAmount,decimals,sourceAt}`.
Ingress validation follows `transfer-ingress.md` (TX01-TX07): normalized full
32-byte transaction hashes, exact nonnegative safe-integer height/log identity,
uint256 raw quantities and strict present timestamps. Missing event time remains
null. Identical duplicates collapse; conflicting duplicates invalidate a new page.
This route likewise returns one source page; it does not accept a global filter
or sort and does not report a complete event count or snapshot.
`GET /api/assets/{id}/liquidations` => `Envelope<Liquidation[]>`.
`Liquidation`: `{id,sequenceId,instrument,side,eventType,sizeRaw,sizeUnit,price,sourceAt}`.
Kraken latest 100 execution history rows are filtered for liquidation/partial liquidation. Empty rows
do not mean 24h zero. Unit remains source-native pending specification evidence.
`GET /api/sources` => provider identities, documentation/terms links, approval gates, observed state.
`Source`: `{name,documentation,terms,enabled,productionApproved,gap,verification?}`.
`terms` is a URL or null when no applicable terms document has been verified;
null renders an explicit unknown state and no terms link, never a guessed URL.
Optional `verification`: `{checkedAt,transportStatus,scope,rightsStatus,evidenceId}`;
`transportStatus` is `reachable`, `unreachable` or `not_verified`. `checkedAt` is
UTC ISO 8601; scope describes the bounded tested sample; `evidenceId` is a relative
record identifier without a filesystem path. This curated build-time technical
review is not live health, activation or permission. It contains no raw provider
response or market measurements. Unknown reviews omit the object, never invent a
successful timestamp. UI displays the review time in Asia/Shanghai.
`GET /api/workspace` => `{watches:Watch[],notes:Note[]}`.

`Envelope<T>`: `{data:T,status:ok|partial|stale|unavailable|unsupported,source,sourceUrl,fetchedAt,
sourceAt,coverage,reason:string|null,nextCursor:string|null}`. `fetchedAt` is last successful fetch for
stale responses; null when never successfully fetched. A page cursor is opaque URL-safe base64 JSON
of approved provider pagination fields, max 2048 characters; reject malformed/injected fields.
Metadata status covers the dataset, not an assertion that every catalog entry is returned.
For table semantics, client filter/sort/export/count apply only to rows supplied
to the shared table. Holders and transfers supply one upstream page at a time;
the next cursor means another provider page is available, not that the preceding
page was globally filtered or sorted. Markets are the configured seven-asset
directory, not all markets. See `table-query-contract.md` for global-query gates.

## Workspace writes

`PUT /api/watches/{kind}/{key}` => `Watch`; `kind` = `asset` or `address`, key = catalog id or 0x40 hex.
`DELETE /api/watches/{kind}/{key}` => 204; idempotent.
`PUT /api/notes/{kind}/{key}` with `{text,visible,version}` => `Note`;
optimistic version (0 for create), stale version 409. `kind` = `asset` or `address`.
`Watch`: `{kind,key,createdAt}`. `Note`: `{kind,key,text,visible,version,updatedAt}`.
Project-record increment: `Note` additionally requires `projectName:string` and
`projectInfo:string` (non-null, empty when unset). Inputs may include either
field as string/null: omitted/null preserves the current field, while an explicit
empty string clears it; creation defaults to empty. Bounds are120/4000 UTF-16
units respectively, matching frontend maxlength and Java validation. They are
private user records, not source-backed entity labels. All fields and visibility
share the same atomic optimistic version/owner/CSRF checks. See PR01–PR08 in
`project-records.md`; old clients can update text without erasing project data.
Hide never deletes note text. Maximum 4000 Unicode characters and 20 watches per kind.
Workspace ownership is inferred exclusively from the authenticated server principal (anonymous local
access uses `local-workspace`). The same entity can be watched/noted independently by different users.
Reads, deletes, capacity limits and note conflict versions apply only to the current owner. No owner
parameter is accepted as an authority. Tests use two authenticated principals against real MySQL.

## Table preferences (R23)

`GET /api/table-preferences/{tableId}` => `TablePreference` for the current owner,
returning defaults with `version:0,updatedAt:null` when no row exists.
`PUT /api/table-preferences/{tableId}` with `{hidden,order,sortKey,desc,pageSize,version}`
=> saved `TablePreference`; CSRF required. Version 0 creates, stale versions return
409. `TablePreference`: `{tableId,hidden:string[],order:string[],sortKey:string|null,
desc:boolean,pageSize:number,version:number,updatedAt:string|null}`.

| Table ID | Default column order / allowed sort and hidden keys |
| --- | --- |
| markets | asset,price,change,marketCap,fdv,volume,turnover,oi,funding,coverage,actions |
| holders | address,chain,balance,proportion,value,change-1H,change-24H,change-3D,change-7D,change-1M,change-1Y,type,actions |
| transfers | time,direction,from,to,amount,price,value,counterparty,hash,block |
| liquidations | time,type,side,size,price,sequence |
| watches | kind,key,created,action |

`order` must contain every column exactly once. `hidden` contains unique known
keys and cannot hide all columns. Page size is 15, 30 or 60. Unknown table IDs,
invalid keys and malformed values return 400. Search and current page are omitted.

On GET, legacy saved layouts retain valid order/hidden settings, page size and
version. Unknown former keys are removed, newly added columns are appended and
an invalid former sort key becomes null. This normalization does not write the
database or increment a version; the next ordinary versioned PUT persists it.
Presentation columns for supply/valuation/history/funding/event-price are not
new claims of API data coverage: their values stay absent until the corresponding
source and metric contract is implemented. See `docs/ui-realignment.md`.
Ownership uses the same principal-derived identity as watches and notes; supplied
owner parameters cannot change it. Config changes apply only after a successful
response. Reload is explicit after conflict or network errors, preserving visible
data and preventing automatic overwrites of another tab's settings.

## Operational routes

`GET /actuator/health` liveness/readiness; expose no DB details to anonymous clients.
SPA deep links return index.html only for known application paths. Unknown API routes remain 404.
Source URLs and hosts are application configuration, never user-controlled request targets.
