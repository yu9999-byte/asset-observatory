# Metric dictionary v1

All monetary and token values cross the API as decimal strings. Missing values are null, never zero.

| Metric | Definition | Unit / limitation |
| --- | --- | --- |
| lastPrice | venue latest traded spot price | quote units per base; USDT is not USD |
| change24hPct | (last/open24h - 1) * 100 | percent; null if open absent or zero |
| volume24hBase | venue rolling 24h traded base quantity | token units; no cross-venue aggregation |
| turnover24hQuote | venue rolling 24h quote turnover | quote token; no USD relabeling |
| openInterestContracts | provider outstanding derivatives contracts | contracts, not tokens |
| openInterestUsd | provider published USD notional, if explicitly supplied | USD, venue + instrument scoped |
| holderBalance | raw token integer / 10^contractDecimals | exact decimal; one chain/contract |
| transferAmount | raw transfer integer / 10^contractDecimals | exact decimal, not USD |
| transferDirection | recipient==address and sender==address => SELF; recipient => IN; sender => OUT | mint/burn endpoints retained |
| eventIdentity | chain:transactionHash:logIndex where available | no assumed uniqueness by hash alone |
| liquidationSamples | returned settled liquidation order details | bounded provider sample; no 24h total or all-market completeness |
| addressMovement | transfers observed for watched addresses | no inferred buy/sell or entity identity |

Provider time and application fetch time are separate. `sourceAt` is null when the provider does not
publish a timestamp for the specific snapshot. Block timestamps describe events, not observation time.
Indexer pages can change while paging; no stable snapshot guarantee. Holder count or total supply
must not be inferred from one page. Token symbols are labels; chain+contract is the identity.
