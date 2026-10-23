# DEX Screener JCT API probe - 2026-10-22

## Scope and source

This bounded read-only probe used the official token-pairs endpoint:

- `GET https://api.dexscreener.com/latest/dex/tokens/0xc477b6dfd26ec2460b3b92de18837fd476ea7549` for the verified Ethereum JCT contract.
- `GET https://api.dexscreener.com/latest/dex/tokens/0xea37a8de1de2d9d10772eeb569e28bfa5cb17707` for the verified BNB JCT contract.

The requests were made through the machine's existing system proxy. Both
returned HTTP 200 with JSON. Each response is archived under
`evidence/acquisition/dexscreener-jct-20261022/`. The Ethereum body is 3,213
bytes with SHA-256
`8C5629AAE5427895E85374475BB61E85760DA1D010164D7B3F527B1B1B4CFF0C`; the
BNB body is 2,836 bytes with SHA-256
`E216A4B2B0AC89C641BB09B84D5DAF0AF0CEC05A1D3EF23B109A4B3C76B068B4`.
The captured response bodies contain no provider observation timestamp. The
first Ethereum response's HTTP headers advertised a 30-second cache lifetime
and reported `Age: 29`; this is not a source freshness SLA.

The official API reference describes the token-pairs endpoint at 300 requests
per minute. It exposes DEX/pool identity, token contracts, USD/native prices,
transaction counts, volume, price change, liquidity, FDV and market cap where
available. This probe validates only the two requested JCT contracts, not a
market-wide directory or continuous availability.

## Observed records

At retrieval time, the Ethereum contract returned three rows. Two Uniswap v4
rows had the exact JCT contract as `baseToken` and USDT as `quoteToken`, with
`priceUsd` 0.004478 and 0.004447, 24-hour volume $7,430.69 and $1,601.21,
liquidity $12,845.95 and $5,037.13, and FDV $177,727,097 and $176,505,967.
The third row was a different token (`ESTEE`) with JCT as the quote token;
its `$0.0000000002725` `priceUsd` describes ESTEE, not JCT. A consumer must
check both token addresses and the side of the pair before interpreting price.

The BNB contract returned two JCT/USDT rows, on Uniswap and PancakeSwap. Their
`priceUsd` values were $0.004323 and $0.004313; 24-hour volume was $86,938.84
and $1,484.18; liquidity was $7,015.33 and $1,016.73; FDV was $44,597,451
and $44,489,336. The Ethereum and BNB pools therefore disagree materially on
volume and FDV, and the two Ethereum pools also disagree materially on FDV.
This is a multi-pool observation, not a CoinAnk-aligned consolidated quote.

The precise pool addresses and every returned field can be reproduced from the
archived JSON. The values are a single observation and must not be represented
as current now, a complete JCT market, or CoinAnk-equivalent data. No price
history, event-time price, CEX market, OI, funding or liquidation data was
provided by this endpoint.

## Terms and decision

The official API documentation page
`https://docs.dexscreener.com/api/api-terms-and-conditions` was retrieved at
2026-10-22 04:30 UTC through the system proxy. It states the terms were last
updated August 18, 2023. Section 1 grants a limited, revocable API-services
license and prohibits using the API to build or market a product that directly
competes with DEX Screener. Section 2 prohibits making the API Services or any
portion of them available to third parties. Section 3 says users gain no
ownership of the API or data obtained through it. Section 4 permits commercial
and non-commercial API use subject to the agreement's limitations.

Commercial API use alone does not expressly grant rights to embed, cache,
redistribute, or expose the obtained market data to this product's external
users. Because Section 2 and Section 3 leave that scope unclear, treat external
display/storage/cache/derived-use rights as unverified. Keep the source disabled
until DEX Screener confirms those rights in writing. No adapter or runtime
configuration was changed.

The homepage and terms were reachable through the system proxy. Jina Reader
returned HTTP 401 `AuthenticationRequiredError` for the docs and homepage from
this host; this is a reader-access restriction, not a DEX Screener API failure.
