# CoinGecko market-directory alternative probe - 2026-10-22

This is a bounded, read-only probe of the official CoinGecko API reference and
one live anonymous directory request. It does not enable a runtime source or
establish external data-use rights.

## Results

| UTC observation | Request | Result |
| --- | --- | --- |
| 04:06 UTC, bounded request | `https://docs.coingecko.com/reference/coins-list` | HTTP 200, 394,452 bytes transferred in 1.03s. This establishes page reachability only; the HTML was not retained from that initial response, so endpoint parameters and policy text were not independently extracted in this run. |
| 04:08:39 UTC | `GET https://api.coingecko.com/api/v3/coins/list?include_platform=true` | HTTP 429, 187-byte JSON body: `{"status":{"error_code":429,"error_message":"You've exceeded the Rate Limit. Please visit https://www.coingecko.com/en/api/pricing to subscribe to our API plans for higher rate limits."}}`; `Retry-After: 2`. No directory records or count were returned. |
| 04:08 UTC, bounded request | `https://www.coingecko.com/en/api_terms` | Timed out after 15.52s with HTTP `000` and no response body. No rights were established. |

The requests used the host's existing local HTTP proxy and retained TLS
certificate validation (`--ssl-revoke-best-effort` only tolerates unavailable
certificate-revocation endpoints). The official reference host was reachable
once, while later requests to the same documentation/terms surfaces stalled;
this is intermittent transport evidence. The 429 is an actual provider response
from the live API endpoint, not a transport failure or an empty list.

## Decision

Do not use the anonymous endpoint as the product directory. Do not infer that a
CoinGecko catalog equals CoinAnk's spot or perpetual universe, and do not infer
an 800+ count from documentation. An API key/plan, current service limits,
external display/storage/re-distribution rights, canonical chain/contract
identity, and a same-time/same-market-scope comparison are still required.
No account was created and no plan was purchased.

## Reproduction

From PowerShell, with the already-configured local proxy available:

```powershell
curl.exe -sSL --proxy http://127.0.0.1:7897 --ssl-revoke-best-effort `
  --connect-timeout 8 --max-time 30 `
  'https://api.coingecko.com/api/v3/coins/list?include_platform=true'
```

The captured response headers and body are in
`acquisition/coingecko-directory-probe-20261022/`.
