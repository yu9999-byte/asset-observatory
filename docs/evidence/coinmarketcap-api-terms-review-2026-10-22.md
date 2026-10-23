# CoinMarketCap API terms review - 2026-10-22

## Requests and captured documents

This bounded official-document review used GET over the existing workstation
HTTP proxy at `127.0.0.1:7897`, with TLS verification enabled. No account was
created, no API key was requested, and no market-data endpoint was called.

| UTC observation | URL | Result | Body evidence |
| --- | --- | --- | --- |
| About 05:03 UTC | `https://coinmarketcap.com/api/documentation/v1/` | HTTP 301; a bounded redirect-following GET reached `https://coinmarketcap.com/api/documentation/` with HTTP 200 in 0.642 s | 78,800 bytes; SHA-256 `0D3F4BD834CD13916ACC4A9D0063A7156F4FB9E157F130898D2BD55F7C78D10D`; saved as `acquisition/coinmarketcap-terms-20261022/api-docs-follow.raw` |
| About 05:04 UTC | `https://coinmarketcap.com/terms/` | HTTP 200 in 0.694 s | 385,913 bytes; SHA-256 `CEC3BF46FD9429704C0A4DDBF381B8B92BBF84C330EC6950911548920CE24BF2`; saved as `acquisition/coinmarketcap-terms-20261022/general-terms.raw` |

An initial request to `/api/terms/` returned HTTP 404 (17,072-byte error
document, SHA-256
`F321ECA175EA937A14084BFBFEA516EABD2D11546321151D5A887BD2D4294A75`). The
official documentation JSON-LD points to `/terms/` as its `termsOfService`;
the 404 route was not treated as API terms.

## Findings

The official API documentation says developers may start with a free plan,
while commercial and professional tiers unlock broader access, history, and
enterprise-grade licensing. It describes one-minute refresh for most plans;
this marketing statement is not an endpoint-specific freshness or SLA promise.

The official Terms of Use identify their last update as 2025-11-24. They say
the Service is strictly for personal, non-commercial use unless a separate
commercial agreement is entered. The license is personal use only. The
prohibited-activities section bars aggregating content to make it available to
third parties, free or paid distribution/access, and downloading or storing
content unless expressly authorized. The Terms also point out that third-party
data may have additional provider terms. The captured documents do not show
that a free API plan grants this external-user product rights to display,
store/cache, redistribute, or publish derived metrics.

## Decision

Do not enable CoinMarketCap. The free-plan mention does not establish the
required external display, retention/cache, redistribution, or derived-use
rights, while the reviewed Terms of Use are inconsistent with the proposed
public product absent a separate agreement. No asset count, field coverage,
timestamp, quota, cost under a concrete plan, or API response was verified.

Unlock requires a current API plan/credential and written terms or provider
confirmation that explicitly permits external-user display, operational
storage/cache, redistribution, and derived metrics at an affordable cost. Then
recheck exact market fields and reconcile the live directory against CoinAnk
using matching product type, filters, pagination, observation time, and
identity mappings. Do not register or pay as part of this zero-budget review.
