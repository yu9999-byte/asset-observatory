# CoinDesk Data API access and rights review - 2026-10-22

## Scope

This bounded review used GET requests through the workstation's configured HTTP
proxy (`127.0.0.1:7897`) with standard TLS validation. No account was created,
no API key was requested, and no market-data endpoint was called.

| UTC observation | URL | Result | What was observable |
| --- | --- | --- | --- |
| 2026-10-22, about 05:05 UTC | `https://developers.coindesk.com/` | HTTP 200, 7,596 bytes | Nuxt application shell; metadata describes live and historical cryptocurrency data, including prices, social insights, news and blockchain analytics. No rendered plan, quota, price, or API license text. |
| 2026-10-22, about 05:05 UTC | `https://developers.coindesk.com/documentation` | HTTP 200, same 7,596-byte shell | Route metadata and app shell only; no machine-readable API specification or rendered endpoint contract in the response. |
| 2026-10-22, about 05:05 UTC | `https://developers.coindesk.com/pricing` | HTTP 200, same 7,596-byte shell | No plan details, free allowance, pricing or quota could be read from the static response. |
| Prior captured response | `https://min-api.cryptocompare.com/documentation` | HTTP 401 | Body says an API key is required and points to `https://developers.coindesk.com/`; saved at `acquisition/cryptocompare-terms-20261022/api-docs.raw`. |
| 2026-10-22, about 05:05 UTC | `https://www.cryptocompare.com/terms-conditions/` | HTTP 200, 78,298 bytes | Legacy site Terms page, branded CryptoCompare and linking to CoinDesk Data. It describes personal/non-commercial use of that Site and its Content. It is not an API-specific plan or license and does not resolve this product's API rights. Raw page saved at `acquisition/cryptocompare-terms-20261022/terms-conditions.raw`. |
| 2026-10-22, about 05:08 UTC | `https://developers.coindesk.com/documentation` in a temporary background Chrome tab | Navigation failed with `net::ERR_CONNECTION_TIMED_OUT`; the rendered tab showed Chrome's `ERR_CONNECTION_TIMED_OUT` page | Browser route did not expose documentation. The temporary tab was closed. This is host/browser transport failure, not provider outage or absent documentation. |

The workstation could retrieve the public developer page shell, but the
documentation and pricing content did not appear in static HTML. This is a
rendered-content/access limitation for this check. The separate browser route
also timed out. Neither result proves that CoinDesk Data has no plans, no API
documentation, or no data. Do not retry the same browser path unless host
transport changes.

## Decision

Keep CoinDesk Data / CryptoCompare disabled. Available evidence does not
establish a zero-cost plan, usable quota, asset and field coverage, freshness,
or permission for an externally accessible product to display, store/cache,
redistribute, or derive values from API data. The general CryptoCompare Site
Terms cannot be used as a substitute for those API-specific terms.

Unlock requires a publicly readable, current API plan and API-specific terms,
or written provider confirmation covering external-user display, necessary
storage/cache, redistribution and derived metrics, together with the actual
quota, cost and data fields. Only after this gate passes should a bounded,
key-authorized market-data request be made and reconciled against the matching
CoinAnk scope. Do not register, request credentials, pay, or call the data API
as part of this zero-budget check.
