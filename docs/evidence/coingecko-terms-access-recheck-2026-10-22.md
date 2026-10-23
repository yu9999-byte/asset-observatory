# CoinGecko terms access recheck - 2026-10-22

At approximately 2026-10-22 04:38 UTC, three official CoinGecko routes were
checked through the workstation's configured system proxy:

- `https://www.coingecko.com/en/api_terms` returned HTTP 403.
- `https://www.coingecko.com/en/api/documentation` returned HTTP 403.
- `https://www.coingecko.com/en/terms` returned HTTP 403.

The API terms path was then opened once in a newly created background Chrome
tab using the existing browser proxy. That browser path ended at
`chrome-error://chromewebdata/` with `ERR_CONNECTION_TIMED_OUT`; no terms text
was obtained. The tab created for this check was closed afterward. The saved
HTTP response body for the terms route is
`evidence/acquisition/coingecko-terms-recheck-20261022/api-terms-response.html`:
1,451,547 bytes, SHA-256
`5292F6BF08AD7B713D01261DC01AA2D2CD4880F84133CEDB81FF0E856DF1B846`. Its title
is `Challenge Verification | CoinGecko` and it has `noindex,nofollow`; it is
not the API terms document.

Decision: CoinGecko's external display, storage/cache, redistribution and
derived-metric terms remain unknown. This is an access restriction on the
tested routes, not evidence that CoinGecko grants or denies the requested
rights. The existing anonymous coin-directory probe returned HTTP 429, so no
current directory coverage was observed. Do not enable CoinGecko or claim
CoinAnk-equivalent coverage. Unlock requires readable official API terms or
written provider clarification, plus an eligible data access path and a
same-time, same-product-type identity-aware directory reconciliation.
