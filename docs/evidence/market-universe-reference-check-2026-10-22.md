# Market universe and JCT entity reference check — 2026-10-22

This bounded browser observation records what was visible from the current
host; it is not a provider API check, an entitlement grant, or product data.

## CoinAnk market universe

- Reference: <https://coinank.com/zh>
- Observation: 2026-10-22 03:34:58 UTC (11:34:58 Asia/Shanghai)
- Route: web-access Chrome background tab; no existing user tab was touched.
- Result: the public homepage rendered its market/derivatives dashboard and
  table headings. The market table rows were still an empty loading skeleton in
  the accessible DOM. No total asset count, stable pagination, filter scope,
  provider timestamp, or machine-readable universe was observed.
- Conclusion: the user's reported 800+ assets is a required comparison target,
  not independently verified by this observation. No count is inferred from
  visible sample cards, navigation or ranking widgets.

### Follow-up observation

- Observation: 2026-10-22 04:51:59 UTC, using a new read-only web-access
  background tab. The temporary tab was closed after inspection.
- The homepage rendered dashboard summaries for 24-hour turnover, aggregate
  open interest, 24-hour liquidations, long/short ratio and BTC market share.
  It also rendered movers, spot-flow rows, liquidation events, Hyperliquid
  positions and news text. These page observations have no retained provider
  request, raw response, or source timestamps and are not authorized product
  data evidence.
- The main `加密货币现货与合约市场总览` area showed spot/contract tabs and
  metric headings, but its market rows remained blank loading placeholders
  after the rest of the dashboard had rendered. No current asset count,
  stable pagination, or downloadable catalog was exposed in the visible page.
- Conclusion: public CoinAnk UI content confirms that the site itself presents
  a broader live dashboard than this application, but it still does not prove
  the claimed 800+ market universe or permit collecting/redistributing any
  displayed fields. The paid, key-gated official directory endpoint remains
  the only documented route found for a scoped SPOT/SWAP currency list.

## Arkham JCT entity

- Reference: <https://arkm.com/explorer/entity/761cb05a-1839-4662-8c6e-411422adb8b6>
- Observation: 2026-10-22 03:34 UTC, approximate; the Chrome page completed at
  `chrome-error://chromewebdata/` without a provider HTTP response or page data.
- Route: web-access Chrome background tab. One navigation retry produced the
  same browser network error. The temporary tab was closed; no user tab changed.
- Conclusion: entity labels, balances and flow fields were not re-verified.
  This is a host/browser route failure, not evidence that Arkham lacks the data
  or that the entity does not exist. The user-supplied entity ID remains a
  comparison reference only until identity and eligible machine-readable data
  are independently checked.

### Follow-up observation

- Observation: 2026-10-22 03:39 UTC (11:39 Asia/Shanghai), using the same
  read-only Chrome background session.
- The page loaded with title `2015.12.11转出200m | Arkham` and displayed the
  entity name `2015.12.11转出200m`, one address, a zero-dollar aggregate value,
  and the label `MultiSig / Contract Wallet` with a signer address. The page
  displayed four transfers and a section labelled `链上持有量`, with no asset
  rows (`未找到资产`). The text also displayed a link labelled `API`.
- The displayed counterpart addresses included
  `0x47a858b6460a84F3c26e15eBf584ab5BD50eFcCd` and
  `0xbd0Db4CeADB818d989170015f386FF1958aFC959`; no JCT token identity or
  contract mapping was visible in the captured page text.
- This supplied entity URL therefore does not currently establish the Arkham
  JCT token/entity mapping assumed in the earlier acquisition plan. It may be
  an address-flow reference, but that cannot be inferred from the page alone.
  Entity labels and displayed transfers remain UI observations, not an API
  entitlement or permission to collect, store, or redistribute Arkham data.

## CoinAnk API document follow-up

At 2026-10-22 03:40 UTC (11:40 Asia/Shanghai), the official API documentation
was reachable at <https://api-cn.doc.coinank.com/api-394180353>. It defines
`GET /api/baseCoin/list` with required `productType=SWAP|SPOT` and required
`apikey`; the endpoint is marked Package 1. The page's response is a static
example of ticker strings and was last modified eight months earlier. The
pricing page displayed $30/month for its lowest API tier and a seven-day trial
by contacting support. No API call, registration, key or payment was made.
Current count, schema response, quota and external redistribution rights remain
unverified. Full details and unlock conditions are in
`coinank-supported-assets-doc-review-2026-10-22.md`.

## Repository comparison

`src/main/java/io/observatory/Catalog.java` contains seven fixed entries.
`docs/api-contract.md` currently defines market-query completeness over the
configured seven-asset directory. That behavior cannot satisfy the user's
requested broad market universe and must not be presented as complete-market
coverage. Requirement DA11 in `docs/data-acquisition.md` now requires a timed
universe comparison, retained pagination/filter scope, and identity-based
reconciliation before a complete claim. No source was enabled and no runtime
market data was changed by this check.

## Access tooling

`agent-reach doctor --json` could not run because `agent-reach` is not installed
or on PATH in this host. web-access dependency check passed with Chrome and its
CDP proxy available. This report uses only its public-page observation; no
undocumented endpoint was inspected or called.
