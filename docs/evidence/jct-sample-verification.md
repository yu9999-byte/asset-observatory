# JCT real sample reconciliation: 2026-10-21

This is development sample evidence, not live-flow or production acceptance.
The adjacent JSON records six captured HTTP 200 responses, fetch-time basis,
source URLs and response SHA256. Reproduce the comparison with PowerShell 7.5+
using `scripts/verify-jct-samples.ps1`; it reads the captured responses, without
fetching new data or enabling a collection policy.

Blockscout and Ethplorer agree on Ethereum contract
`0xc477b6dfd26ec2460b3b92de18837fd476ea7549`, symbol JCT, 18 decimals and
deployment raw supply `39685177488465018509308769880`. This is not the project's
global genesis allocation or circulating supply. No cross-chain sum or FDV is
derived. The verified Ethereum deployment is now in the runtime catalog; spot
and swap instrument IDs remain null.

Of the 30 Ethplorer holder rows compared against the 50-row Blockscout page,
29 raw integer balances match exactly, including the first five. Address
`0x000000000004444c5dc75cb358380d2e3de08a90` differs:

| Source | Raw balance |
| --- | --- |
| Ethplorer | 1220807200858034932256847 |
| Blockscout | 1275693154225152005648531 |

Holder counts also differ: Ethplorer 690 versus Blockscout 687. Neither response
pins these holder pages to a shared block. The cause of the discrepancy is
unresolved; do not silently merge the pages, select a winner or call this a
verified complete Top30 snapshot. Source snapshot times remain unknown.

For sample address `0xa68f0255c0eeb35e3cf558d58cfbbfa8811b9b84`, all four
Blockscout transfers match Routescan on transaction hash, contract, raw quantity,
decimals, From, To, block number/hash and event time. Routescan does not return
log_index here, so independent event-log identity is unverified. These old
events do not prove continuous collection, finality, backfill or event-time USD
prices.

Ethplorer freekey is a development source. Blockscout legacy sustainability and
Routescan production storage/display conditions remain unresolved. Runtime
sources remain disabled; the Java/MySQL/UI live-data flow, JCT market mapping,
derivatives and historical coverage still require acceptance evidence.
