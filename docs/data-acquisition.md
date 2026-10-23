# Data acquisition contract — 2026-10-22

User-approved order: prove data access, freeze coverage, specify the solution,
then implement. MySQL is a persistence choice, not a data provider or a mandatory
hop for every live request. Existing application sources remain policy-gated.

## Acquisition acceptance

| ID | Required evidence | Pass condition |
| --- | --- | --- |
| DA01 | Reproducible read-only HTTPS probes | Exact request, Java runtime, UTC start/end, elapsed time, status/error, raw body SHA256 and bounded response saved locally |
| DA02 | Aggregated quotes | Provider asset ID and name checked; decimal strings preserved; invalid/missing fields identified individually; no invented snapshot time |
| DA03 | ERC-20 holders | Chain/contract/decimals checked; cursor and observed scope retained; one page does not establish a fixed-block Top30 |
| DA04 | ERC-20 transfers | Contract, transaction hash, block hash/height, log index, raw amount, addresses and event time verified; transactionIndex is not logIndex |
| DA05 | JCT derivatives | Venue instrument and asset identity, units, OI/funding/volume timestamps and liquidation coverage proven independently |
| DA06 | External-use rights | Explicit scope for display, storage and derived metrics; key/plan/region/quota recorded; HTTP 200 alone never passes this gate |
| DA07 | Failure and alternative | Distinguish transport, HTTP, provider/schema and empty results; document at least one relevant alternative for blocked critical fields |
| DA08 | Architecture decision | Choose query/cache/history only after DA01-DA07; persisted user records and approved histories use MySQL |
| DA11 | Market universe | Compare the product asset universe against the user-selected CoinAnk reference at a recorded time; retain source IDs, pagination, filters and count reconciliation; never label a fixed internal catalog as the complete market |

## Initial identity and scope

JCT means Janction, Ethereum mainnet contract
`0xc477b6dfd26ec2460b3b92de18837fd476ea7549`, 18 decimals. BNB is a separate
deployment and is not silently mixed into Ethereum balances. CoinLore directory
ID `183005` is a candidate provider mapping, not proof that the provider links
that ID to this contract. JCT venue mappings remain unverified until corroborated.

Retain BTC, ETH, LINK, UNI, USDC and WBTC as quote comparisons. BTC and native
ETH do not acquire ERC-20 holder coverage through this investigation.

## Market universe and reference alignment

The user requires the market homepage to align with CoinAnk's displayed asset
universe (reported by the user as 800+ assets) and supplied Arkham entity
`761cb05a-1839-4662-8c6e-411422adb8b6` for comparison. The 800+ figure is a
target to verify, not an independently measured count. A current page
observation shows that this Arkham entity is named `2015.12.11转出200m`, has
one address and four displayed transfers, and does not expose a JCT contract
mapping in its visible fields. Do not treat it as the JCT token entity until
the identity is reconciled. The current Java catalog has seven entries. The
current market-query API correctly reports this configured directory as
`partial`, but it still contains no live market measurements and has not been
reconciled to CoinAnk's broader market universe.

CoinAnk is the comparison benchmark, not an assumed ingestion entitlement or
source of truth for runtime data. Before implementation, capture its visible
asset count and filters/pagination, identify an eligible machine-readable
directory source, and reconcile the two universes by provider identity and,
where applicable, chain plus contract address. Symbol/name similarity alone is
not an identity match. Report listings that are excluded, duplicated, delisted,
or unmapped along with the provider timestamp and coverage; only the reconciled
scope may be called complete. Do not activate undocumented endpoints or scrape
restricted pages to force a count match.

CoinAnk's official docs expose `GET /api/baseCoin/list` (`productType=SPOT|SWAP`)
as a Package 1 endpoint requiring a registered, activated API key. Its static
ticker-only example is not a live directory, and page visibility does not grant
external display, storage, redistribution or derived-use rights. The current
zero-budget scope cannot rely on this endpoint until those access and rights
conditions change; details are in
`evidence/coinank-supported-assets-doc-review-2026-10-22.md`.

For JCT, the supplied Arkham entity page is only a comparison candidate until
its identity is reconciled: the 2026-10-22 observation showed a named,
one-address transfer entity and did not expose a JCT contract mapping. Record
the entity/token identity mapping, chain, contract, source timestamp, coverage
and field-level differences before treating it as JCT. An Arkham display is
not by itself an API entitlement or proof that labels and balances can be
redistributed. Never infer an entity label from balance size.

## Probe rules

Use an explicit allowlist and GET only, with TLS verification, no redirects,
bounded bodies, one attempt per target per transport, per-request deadlines and
no concurrent requests to a provider. No login, automatic registration, purchase,
geographic bypass, wallet/signing or trade endpoints. An optional existing HTTP
proxy is an operator transport input; never print credentials or write keys.

Save raw samples as local development evidence only. A successful probe, parser
test or self-reported provider timestamp is not permission, freshness, finality,
continuous coverage or production acceptance. Samples never feed runtime APIs.

The Java live probe is opt-in and skipped in the ordinary test suite. Failure to
reach a source is recorded rather than converted to a fake fixture or zero. Its
test completion proves the report was generated, not that any provider passed.

## Mapping contract before implementation

CoinLore: check exact ID/symbol/name; normalize valid decimal fields separately;
preserve the raw response, record strings such as `0?` as invalid, keep valid
zero values as zero, and do not infer `sourceAt` from observation time. Never
derive token quantity from USD turnover, FDV from unknown supply, or a rolling
window from a calendar-day open.

Routescan logs: require a 32-byte Transfer signature, exactly three topics,
zero-padded address topics, 32-byte amount, full transaction/block hashes and
nonnegative hex height/index. Convert uint256 with BigInteger and token units
with BigDecimal; retain burn/mint endpoints. Event identity includes chain,
transaction hash and log index. Reconcile indexed transfers against all shared
identity/amount/time fields; missing log identity cannot be replaced by a
transaction index. Same-provider endpoint agreement is not independent-source
reconciliation.

## Dependency checklist

- [x] User confirmed this execution order and zero-budget/external-use scope.
- [x] DA01-DA08 and parser behavior specified before implementation.
- [x] Run Java direct probes and archive evidence; existing system proxy is disabled, so that transport was not used.
- [x] Normalize real quote fields and reconcile 4 indexed transfers to raw logs; report holder/derivative gaps.
- [x] Explicitly block unresolved external-use rights, full holder scope and JCT derivatives.
- [x] Bounded alternative quote checks recorded: DexScreener and GeckoTerminal direct HTTPS both timed out before HTTP response; neither was treated as missing data or enabled.
- [x] Record verified field coverage and update SDD/API/architecture decisions without reducing the original backlog.
- [ ] Verify CoinAnk's visible market-universe count and scope; reconcile a permitted live directory against it without treating the reported 800+ figure as already proven.
- [ ] Reconcile the supplied Arkham JCT entity fields and identity against eligible machine-readable data; preserve discrepancies and rights gaps.
- [ ] Implement only entitled adapters; complete remaining R01-R24 tests/drills.

Full run: `evidence/acquisition/20261021T162159001Z-direct/report.json`.
Targeted reconciliation: `evidence/acquisition/20261021T162534395Z-direct/report.json`.
Interpretation, rights references and unlocks: `evidence/data-acquisition-2026-10-22.md`.
Offline `AcquisitionEvidenceTest` verifies retained hashes and replays the actual
normalization/matching; it does not invoke sources or prove continuous availability.

No new source is approved or enabled by this document.

## Bounded direct recheck - 2026-10-22

At 02:02-02:03 UTC, an opt-in Java GET-only run obtained HTTP 200 from
Routescan's indexed transfer page (four rows) and raw-log endpoint. The latter
queried one fixed historical block and matched one row; the other three
transfers were outside that query, so this is not full-page reconciliation.
Blockscout and Ethplorer holder calls failed with `ConnectException` and no HTTP
status/body. At 02:06-02:07 UTC, the core run captured CoinLore HTTP 200 for
seven directory assets (raw SHA256
`9ea0ccd0c701e3238b8d5b4670250cbd57a13fa43cbb99950ac3a63489a383e2`), while
Routescan transfers/logs and Blockscout holders all failed during connection.
JCT still has no quote time; market cap and circulating supply remain invalid
`0?`, total supply is absent. Exact URLs, timestamps, hashes, schemas and
transport errors are in `evidence/acquisition/20261022T020500Z-recheck/` and
`evidence/acquisition/20261022T020800Z-core-recheck/`. These observations show
intermittent host reachability, not source-side uptime or entitlement. CoinLore
and Routescan remain disabled because external display/storage terms have not
been established.

## R09 source-review API increment

DA10 (2026-10-22): a later official terms review must be visible without
changing an earlier market observation time or granting activation. CoinLore's
captured section 6 requires express written consent for redistribution or
commercial use; until clarified/obtained, external display remains blocked.
MEXC's corrected legacy-documented public contract URL is a separate candidate
review with actual deployment transport failures, not a verified JCT listing.
The evidence ID for a combined review must resolve to a dated audit document
linking raw archives and their request times. No document timestamp is a quote
timestamp. Verify via AcquisitionEvidenceTest and the actual /api/sources response.
An unverified terms URL is null and displays as unknown, without a link.

`GET /api/sources` retains its existing fields and may add a `verification`
object: `checkedAt` (UTC ISO instant), `transportStatus` (`reachable`,
`unreachable`, `not_verified`), `scope` (tested fields/window), `rightsStatus`
(review status, never inferred from transport), and `evidenceId` (local report
identifier, no filesystem path or raw market data). The review is build-time,
dated evidence rather than a live health guarantee. `enabled` and
`productionApproved` remain independent. No raw response, quote, address balance
or key is included. The UI shows these independent states and the unlock reason.

## CoinAnk JCT derivative discovery — 2026-10-22

A read-only browser review of the CoinAnk JCT perpetual page observed six venue
rows and aggregate OI values with multi-period percentage changes. The separate
API route rendered an empty body. The public terms restrict over-scope scraping
and do not expressly grant external redistribution/storage rights. This is page
discovery only; no undocumented endpoints were inspected or called. The page
does not prove venue completeness, contract mapping, unit methodology, data time,
history continuity or entitlement. See
`evidence/coinank-jct-page-review-2026-10-22.md`. Do not enable it without an
official API sample, instrument/metric specification and explicit external-use
authorization.

## Holder index transport recheck - 2026-10-22 02:22 UTC

Using the project-pinned JDK 21/Maven 3.9 toolchain, the allow-listed Java probe
retried Blockscout's documented JCT holders endpoint and Ethplorer's token-info
and Top-30 endpoints. All three failed with `ConnectException` before HTTP
status/body. The report and reproduction are in
`evidence/holder-source-recheck-2026-10-22.md`. This updates Java-host transport
evidence only; it is not an empty result, provider outage proof, a Top30 snapshot,
or permission to retain/display data. No adapter or source policy was enabled.
