# CoinPaprika directory alternative probe - 2026-10-22

This is a bounded, read-only feasibility probe of CoinPaprika's public API. It
does not enable a runtime source and does not establish the right to display
data to external users.

## Observed responses

| UTC observation | Request | Result |
| --- | --- | --- |
| 04:09:37 | `GET https://api.coinpaprika.com/v1/coins` | HTTP 200; 7,557,005 bytes; `last-modified: 2026-10-22 04:08:10 UTC`; cached response age 86 seconds. The captured list contains 62,319 rows: 12,245 `is_active=true`, 12,235 with `rank > 0`. Response headers advertised `ratelimit-limit: 20000` and `ratelimit-remaining: 19957`. |
| 04:10:33 | `GET https://api.coinpaprika.com/v1/coins/jct2-janction` | HTTP 200; response names Janction/JCT and maps Ethereum contract `0xC477B6dfd26EC2460b3b92de18837Fd476Ea7549` plus BNB contract `0xea37a8de1de2d9d10772eeb569e28bfa5cb17707`. Both match the separately verified JCT deployments. The response reported rank 393 and `last_data_at=2026-10-22T04:10:00Z`. |
| 04:10:15 source timestamp, retrieved in this probe | `GET https://api.coinpaprika.com/v1/tickers/jct2-janction` | HTTP 200; USD price `0.004302779885327758`, 24h volume `11648745.10150784`, market cap `49454807`, `last_updated=2026-10-22T04:10:15Z`; rank 391. The rank differs from the nearby coin-detail response, so do not treat ranks from separate calls as a synchronized snapshot. |
| 04:09–04:11 bounded terms/docs requests | `https://coinpaprika.com/terms-and-conditions/`; `https://docs.coinpaprika.com/` | The guessed terms path returned HTTP 404 and the docs host timed out after 15.30 seconds. Official `robots.txt` and sitemap were then read; the sitemap pointed to the API licensing page. |
| 04:13:25 UTC | `https://coinpaprika.com/api/licensing` | HTTP 200, 68,183 bytes. The page says Enterprise is the only plan that permits redistribution or sharing CoinPaprika data, requires a dedicated contract, and defines customer-facing display/distribution rights. It says Keyless is personal use and not for a commercial product; Starter through Ultimate are for internal company use. It also says standard API terms require a “Powered by CoinPaprika” credit. |

Requests used the pre-existing local HTTP proxy with TLS certificate
validation enabled. The directory and JCT responses are genuine provider
responses. They establish sample endpoint reachability and one identity-matched
quote, not continuous availability, historical coverage, market-universe
equivalence, or external-use permission.

## Interpretation and decision

The 62,319-row response includes many assets that are not shown to be listed on
the same spot or perpetual markets counted by CoinAnk. Therefore neither 62,319
nor 12,235 is a valid substitute for CoinAnk's user-reported 800+ market
universe. A same-time comparison with matched product types, listing filters
and active-status rules has not been completed. The list is useful for
candidate discovery; the JCT detail endpoint provides strong chain/contract
identity evidence beyond a ticker match.

CoinPaprika is technically reachable without a key in this bounded probe, but
its official licensing page explicitly reserves customer-facing sharing and
redistribution for an Enterprise contract. Keyless access is personal-only;
the lower paid plans are internal-use only. This does not satisfy the current
zero-budget external-user scope, so the source must remain disabled. The unlock
condition is a negotiated Enterprise agreement that expressly covers the
planned customer-facing display, storage/caching and derived metrics. The
ticker is one current quote, not a validated refresh SLA or historical series.
No CoinAnk/Arkham value was substituted, and no account was created or plan
purchased.

## Captures

Raw responses and headers are under
`acquisition/coinpaprika-directory-probe-20261022/`.

| File | SHA-256 |
| --- | --- |
| `coins.json` | `4227A653A4AE37875EAA70700423286C38A39D05DD6B45AC734B5D0B147E2834` |
| `jct-coin.json` | `C5E9D276B51DAFDDCA45914C280FF39D3011D752F22F9432D309C7492D1477FD` |
| `jct-ticker-recheck.json` | `792FB127328E34340382E3B5B577F247B1B7DD08627F06EDD0D20B6415BDCEE2` |
| `terms-404.html` | `0770246AD075C1C1540D57114F1A5DF9AE57E7699EB43D550D62A6521730F0ED` |
| `api-licensing.html` | `24F2A264817E1C5E0C8AC76DF9898F3739E9EDB276F0312806FFCC96347A6A20` |

## Reproduction

```powershell
curl.exe -sSL --proxy http://127.0.0.1:7897 --ssl-revoke-best-effort `
  --connect-timeout 8 --max-time 20 `
  -D coins.headers -o coins.json https://api.coinpaprika.com/v1/coins

curl.exe -sSL --proxy http://127.0.0.1:7897 --ssl-revoke-best-effort `
  --connect-timeout 8 --max-time 20 `
  -D jct-coin.headers -o jct-coin.json `
  https://api.coinpaprika.com/v1/coins/jct2-janction

curl.exe -sSL --proxy http://127.0.0.1:7897 --ssl-revoke-best-effort `
  --connect-timeout 8 --max-time 20 `
  -D jct-ticker.headers -o jct-ticker.json `
  https://api.coinpaprika.com/v1/tickers/jct2-janction
```
