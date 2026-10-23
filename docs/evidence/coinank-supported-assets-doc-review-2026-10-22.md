# CoinAnk supported-assets API document review — 2026-10-22

This is a read-only review of CoinAnk's public API pricing page and official API
documentation. It does not call the documented API or grant use rights.

## Official documentation

- API docs: <https://api-cn.doc.coinank.com/api-394180353>
- API pricing and product description: <https://coinank.com/zh/openApi>
- Observation: 2026-10-22 03:40 UTC (11:40 Asia/Shanghai), via the web-access
  Chrome background tab.
- Endpoint: `GET /api/baseCoin/list`
- Query: `productType`, required, `SWAP` or `SPOT`.
- Header: `apikey`, required; documentation says to register and activate to
  obtain it.
- Product tier: the page labels the endpoint Package 1. The pricing page shows
  the lowest API package at $30/month, with a seven-day trial available by
  contacting support. The trial is not self-service and is not evidence of a
  zero-cost ongoing production entitlement.
- Response example: a JSON array of ticker strings, not a live response. The
  page says the endpoint returns all supported currencies for the selected
  product type. The page was last modified eight months earlier, so the sample
  cannot establish today's count or listing scope.
- Related documented APIs include market-cap and supported-pair endpoints.
  Their presence does not prove coverage for spot cap, circulating supply,
  chain/contract identity, price history, or external display/storage rights.

## Decision

This endpoint is a strong candidate for reconciling CoinAnk's own supported
tickers by product type after access is granted. It cannot currently be treated
as a verified live 800+ directory: the homepage did not render a count in the
current observation, the API response was not called, the documented example
is static, and the API identity is ticker-only. Do not hardcode its sample or
silently join symbols to chain assets.

No API key was available or requested, no registration or payment was made, and
no endpoint was called. External users need documented permission for public
display, storage, redistribution, derived values, and any attribution. These
conditions are not established by public endpoint documentation or page
visibility.

## Unlock conditions

1. Operator obtains written confirmation of API access and permitted external
   display, caching/storage, redistribution, attribution, and derived use.
2. Operator provisions a key and confirms current package price, rate limits,
   region, supported products, and trial duration.
3. Run one bounded live `SPOT` and `SWAP` directory request through the
   documented schema; retain timestamp and raw response hash.
4. Reconcile returned tickers and their instrument scope against the CoinAnk
   page at the same time. Resolve aliases, duplicate symbols and delistings;
   enrich chain assets only with a separate verified chain/contract mapping.
5. Only then decide whether the directory belongs in the externally served
   product. A paid API key does not by itself establish redistribution rights.
