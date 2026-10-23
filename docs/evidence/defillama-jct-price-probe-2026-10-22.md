# DefiLlama JCT price API probe - 2026-10-22

## Requests and observations

Bounded read-only requests were made through the workstation's existing system
proxy to the official Free API and official API documentation. No account,
API key or payment was used.

Raw responses are preserved in
`evidence/acquisition/defillama-jct-20261022/`.

The official API docs index at `https://api-docs.defillama.com/llms.txt`
returned HTTP 200 (9,228 bytes; SHA-256
`CB0A290FFB958A034D57858AEB24AEBB2711AEBFE9F19A33CD042DBB66BAFCD7`). It
states that the Free API needs no authentication, has 31 endpoints, and is
separate from the paid Pro API. The current-price endpoint is
`https://coins.llama.fi/prices/current/{coins}`.

The exact-contract requests below each returned HTTP 200:

| Chain and request identifier | Response fields |
| --- | --- |
| Ethereum `ethereum:0xc477b6dfd26ec2460b3b92de18837fd476ea7549` | symbol `JCT`, decimals `18`, price `0.004302752817872832`, timestamp `1792643223` (2026-10-22 04:27:03 UTC), confidence `0.99`; 166 bytes; SHA-256 `A36217F5788401705CDF249FA271546F446B0FE806E9286336A9941CBC021000`; raw: `acquisition/defillama-jct-20261022/ethereum-jct-price.json` |
| BNB Chain `bsc:0xea37a8de1de2d9d10772eeb569e28bfa5cb17707` | symbol `JCT`, decimals `18`, price `0.004302752817872832`, timestamp `1792643223` (2026-10-22 04:27:03 UTC), confidence `0.99`; 161 bytes; SHA-256 `F3C6617A3834822B035549574139AD7CB5751A472987FF13808008D43A819A91`; raw: `acquisition/defillama-jct-20261022/bnb-jct-price.json` |

These are two observed responses, not evidence of continuous availability,
historical coverage, a refresh guarantee, or market-wide inclusion. The sample
does not establish market cap, FDV, turnover, contract open interest, funding,
liquidations, holder rankings, transfers, or event-time prices. No same-time
CoinAnk reconciliation was performed. The identical JCT prices across both
contract queries are recorded as observed; they do not establish that the
provider's aggregation methodology matches CoinAnk.

## Use-rights review and decision

The official terms at `https://defillama.com/terms` returned HTTP 200. The
captured raw response was 29,921 bytes; SHA-256
`399E33214630344997A38B5F08CAAE70E6674B5A9A751A20AD0EE0EEEDBF068D` (file:
`evidence/acquisition/defillama-jct-20261022/terms.html`). Clause 7
grants a revocable license for personal, non-commercial use. Clause 8 prohibits
republishing data in any form without permission and prohibits commercial
programmatic exploitation without prior written consent. The terms expressly
include the official public APIs within the defined Services. Therefore this
external-user product must not display or persist these API data unless
DefiLlama grants permission covering the intended display, storage/cache and
derived metrics. This is a documented use-rights gate, not an API access
failure.

Keep disabled pending written authorization. If authorized, repeat a
timestamp-aligned comparison with the identified CoinAnk JCT markets, document
quote methodology and rate/cache behavior, and only then add a Java adapter.
