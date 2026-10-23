# Ethereum public data verification

Observed on 2026-10-21 UTC. This is sampled real-data evidence, not production approval.

## Result

- Ethereum mainnet ERC-20 LINK, UNI, USDC and WBTC holder pages returned HTTP 200 with 50 records through the existing Chrome network configuration.
- LINK holders page 2 was fetched with the exact returned cursor and returned another 50 records. The first address changed while an equal balance persisted; retain all cursor fields, not balance alone.
- LINK filtered address transfers returned HTTP 200 with 50 records. Latest sampled event timestamp is 2026-10-13T06:52:59Z, while observedAt is 2026-10-21; event age must not be misreported as fetch staleness.
- Independent Ethplorer LINK holder sample and Routescan LINK event sample agreed with Blockscout on exact raw amounts and addresses/transaction hash.
- Direct host Node/curl failed connectivity. Python urllib through the existing Windows proxy returned Blockscout HTTP 403 while independent alternatives returned HTTP 200. No bypass was attempted.

## Adapter contract

Base: https://eth.blockscout.com/api/v2

GET /tokens/{contract}: symbol/name/decimals/total_supply/holders_count. These number-like values are strings; metadata has no source-updated-at guarantee.

GET /tokens/{contract}/holders: items[].address.hash, address.is_contract, address.name, optional metadata.tags, items[].value, token_id, next_page_params. raw value divided by 10^token.decimals gives token units. Use arbitrary precision. Holder labels describe provider annotations, not verified real-world identity. A contract address can represent pooled holdings and must not be called a unique beneficial owner.

GET /addresses/{address}/token-transfers?type=ERC-20&token={contract}: items[].transaction_hash, log_index, block_number, block_hash, timestamp, from.hash, to.hash, token.address_hash, token.decimals, total.decimals, total.value. Cursor example is {index,token,block_number}. Keep the original type/token filter and merge the entire returned cursor. Identity is chainId + transaction_hash + log_index; if a fallback has no log index, mark identity coverage limitation rather than invent it.

Source time is event timestamp for transfers; observedAt is actual request time. Holders have no per-item block/time in these responses, so sourceAsOf must be null/unknown. HTTP Date is transport metadata, not indexed blockchain height. Do not claim finality or freshness from HTTP 200 alone. Track provider indexer head separately. List pagination is not a consistent snapshot guarantee.

## Fixed catalog

| Symbol | Contract | Decimals |
| --- | --- | --- |
| LINK | 0x514910771af9ca656af840dff83e8264ecf986ca | 18 |
| UNI | 0x1f9840a85d5af5bf1d1762f925bdaddc4201f984 | 18 |
| USDC | 0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48 | 6 |
| WBTC | 0x2260fac5e5542a773aa44fbcfedf7c193bc2c599 | 8 |

LINK decimals were verified through Blockscout metadata; the other three through Ethplorer getTokenInfo. All sample hashes/URLs/times are recorded in chain-source-verification.json.

## Production gates and terms

Official Blockscout REST documentation says per-instance /api/v2 sunset was planned for 2026-07-01 and recommends PRO API. That date is already past at verification time. Legacy accessibility is therefore a migration-risk observation, not a production availability commitment. PRO docs require an API key and describe a 5 req/sec free plan. No key was obtained or production entitlement validated.

Blockscout corporate footer links https://eaas.blockscout.com/terms-and-conditions (retrieved; last updated 2025-04-04). This verification did not establish that the terms grant this proposed application's intended persistence or redistribution rights. Status: pending review, not authorized.

Ethplorer homepage labels freekey development-only and states 1 req/sec, 30/min, 100/hour, 1000/day, 1000/week. Homepage FAQ allows commercial projects with attribution and a link, but that does not change freekey's development-only restriction. Use this probe only as development verification; unlock production with an appropriate own key/plan and attribution. Its GitHub documentation contains differing limits; use the stricter homepage limits until clarified.

Routescan public Ethereum etherscan-compatible tokentx endpoint answered without a key and matched the sample. Its commercial/persistence entitlement and quota have not been established here. Status: verified alternative connectivity and sample accuracy, production terms pending. Routescan response lacks a demonstrated log_index, so it cannot silently replace the exact Blockscout event identity contract.

## Official references

- https://docs.blockscout.com/devs/apis
- https://docs.blockscout.com/devs/apis/rest
- https://docs.blockscout.com/devs/pro-api
- https://eaas.blockscout.com/terms-and-conditions
- https://ethplorer.io/
- https://github.com/EverexIO/Ethplorer/wiki/Ethplorer-API
- https://docs.routescan.io/

Production is blocked pending eligible source access/terms, deployment-host connectivity, and independently measured indexing/finality coverage. No exchange trading, transfer, wallet or funding operation was performed.

