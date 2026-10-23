# Market source verification — 2026-10-21

This is feasibility evidence, not a grant to redistribute market data. Full request metadata and selected real responses are in `market-source-verification.json`.

## Decisions

- **OKX: disabled pending written data license.** The official API agreement, published 2026-03-26, sections 9.2–9.4 expressly limits public market-data use and third-party display/redistribution, and restricts competing aggregators/analytics platforms. Public unauthenticated availability is expressly not permission. Do not ship an enabled OKX adapter for this product without resolving that scope.
- **Kraken: technically reachable through the machine's existing Windows system proxy; external release not cleared.** Its global terms limit content use to the user's own benefit and restrict third-party distribution. No affirmative third-party display/data redistribution license was verified. A local single-user research scope and a third-party product scope must be treated separately.
- Exact Kraken locator: Global Terms, `Content; Feedback` → `Our Content` paragraph. Additional purposes require prior permission via `marketdata@kraken.com`; a separate prohibited-use list restricts third-party content provision. This run did not request or receive that permission.
- **No exchange-wide/global liquidation total is verified.** Show a finite observed event sample with explicit window and coverage, or missing. A page returning zero liquidation records is not evidence of zero 24h liquidations.

## Transport evidence

Native Node fetch to OKX failed with EHOSTUNREACH at 169.254.0.2:443; Kraken timed out. Direct curl also failed; `--ssl-revoke-best-effort` did not repair Kraken timeout. Python urllib detected the pre-existing Windows system proxy `http://127.0.0.1:7897`: Kraken Spot and Futures returned HTTP 200; OKX returned HTTP 403. Browser public access succeeded for OKX but is not a production ingestion channel. No login was needed, no region restriction was bypassed, and no TLS validation was disabled. Java must be tested with its own explicit transport settings.

## Verified endpoints and meaning

| Provider | GET endpoint | Required parameters | Meaning / limits |
|---|---|---|---|
| OKX | `/api/v5/market/tickers` | `instType=SPOT` | Venue spot snapshot, 24h base/quote volume; USDT is not USD |
| OKX | `/api/v5/public/open-interest` | `instType=SWAP` | `oi` contracts, `oiCcy` coin, `oiUsd` USD |
| OKX | `/api/v5/public/instruments` | `instType=SWAP` | Contract value, multiplier, settlement currency |
| OKX | `/api/v5/market/candles` | `instId=BTC-USDT` | `bar=1D` is UTC+8; use `1Dutc` for UTC; unfinished candles must be labeled |
| OKX | `/api/v5/public/liquidation-orders` | tested `instType=SWAP&mgnMode=cross&state=filled&uly=BTC-USDT` | Real 100-detail sample; exact endpoint absent from current full v5 docs; no completeness claim |
| Kraken Spot | `/0/public/Ticker` | tested `pair=XBTUSD,ETHUSD,LINKUSD,UNIUSD,USDCUSD,WBTCUSD` | All six returned; response lacks individual source timestamps; `o` is today's UTC open, not rolling 24h open |
| Kraken Futures | `/derivatives/api/v3/tickers` | none | 229 tickers; exact PF_XBTUSD, PF_ETHUSD, PF_LINKUSD, PF_UNIUSD, PF_USDCUSD present; PF_WBTCUSD missing |
| Kraken Futures | `/derivatives/api/v3/history` | `symbol=PF_XBTUSD` | Latest 100 trades; optional `lastTime`; retention is 7d or since engine restart, whichever is shorter |

Kraken Futures `vol24h` is the sum of fill sizes; `volumeQuote` is the sum of size × price. `markPrice` is the venue margin mark. `lastTime` is the last-trade time and can be stale even while `serverTime` is fresh. The ticker description does not specify the `openInterest` unit: retain native units as unverified until product/instrument definitions support conversion. Do not label that raw field USD.

The official history contract defines `liquidation` and `partial liquidation`; covered liquidations use `liquidation`. `assignment` and `unwind` are distinct. The actual BTC sample returned 100 ordinary fills and no liquidation, so a nonzero liquidation reconciliation is still outstanding. Use string `sequence_id` for gap detection where present, and show continuity unknown until the ingestion window is proven.

## Release gaps and unlock conditions

1. Applicable jurisdiction and intended audience must be explicit. Third-party/commercial display needs confirmed data rights; obtain written permission where required.
2. Java HTTPS transport must fetch the allowed endpoint in the deployment network using explicit approved configuration. Browser-only success is insufficient.
3. Kraken derivative native-unit conversion and exact current polling limits require additional official confirmation before USD aggregation.
4. A liquidation event must be observed and reconciled before claiming real liquidation coverage. Complete 24h totals require proven continuous ingestion/backfill without gaps.
5. WBTC derivatives are missing; do not substitute BTC exposures for WBTC.

## Official sources

- https://www.okx.com/docs-v5/en/
- https://www.okx.com/zh-hans/help/okx-api-agreement
- https://www.okx.com/zh-hans/help/terms-of-service
- https://docs.kraken.com/api-reference/market-data/get-ticker-information
- https://docs.kraken.com/api-reference/market-data/get-tickers
- https://docs.kraken.com/api-reference/instrument-details/get-instruments.md
- https://docs.kraken.com/api-reference/market-data/get-trade-history.md
- https://docs.kraken.com/api-reference/market-history/get-public-execution-events.md
- https://www.kraken.com/legal/global-terms

Full payload hashes are recorded, but full raw payloads were not persisted; JSON contains selected genuine response samples. No missing data was replaced with synthetic values.

