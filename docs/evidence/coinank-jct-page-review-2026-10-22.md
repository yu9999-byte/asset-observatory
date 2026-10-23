# CoinAnk JCT page review — 2026-10-22

This is source-discovery evidence from a read-only browser page review. It is
not an API capture, permission grant, independent reconciliation, or runtime
data source. The user's existing browser tabs were left untouched; a separate
temporary tab was closed after review.

## Observation

- Observed at `2026-10-22T02:13Z` using a separate Chrome CDP tab.
- JCT page: https://coinank.com/zh/openInterest/contract/JCT
- The rendered table listed Binance, Bybit, Bitget, Gate, Aster and Huobi.
- The page displayed an aggregate of `$3,285.67万` and approximately `82.62亿
  JCT`; venue rows displayed a USD figure, approximate JCT quantity, share and
  multiple percentage-change columns. The visible text did not preserve the
  exact interval header mapping for every value, and no provider snapshot time
  was exposed. These numbers are retained only as a discovery observation and
  must not be loaded into application state or described as current afterward.
- The page advertises a full-market aggregate, but the observed six rows do not
  prove the covered venue universe, instrument identity, calculation method,
  units, or completeness.

## API and terms

- The page's API navigation points to https://coinank.com/zh/openApi. The route
  loaded with an empty body in this review, so API schema, authentication,
  plan, rate limit, field history and sample response could not be verified.
- Terms page: https://coinank.com/zh/terms. It describes market data and tools
  as informational, prohibits abuse including scraping beyond the permitted
  scope, and reserves content/software intellectual property to CoinAnk or its
  licensors. The reviewed text does not expressly grant third-party
  redistribution or external commercial display rights.
- Decision: do not scrape page internals, call undocumented endpoints, or
  display this source to external users. Reconsider only after official API
  documentation/data access, a verifiable JCT instrument/metric definition,
  timestamp/coverage evidence, and explicit external display/storage permission.

## Reproduction limits

The browser page was rendered and read through the web-access CDP bridge. No
page export, API request, account action, registration or purchase was made.
This observation does not close DA05/DA06 or gate G03/G04. It identifies a
candidate worth an operator permission/API review and does not change the
provider activation flags.
