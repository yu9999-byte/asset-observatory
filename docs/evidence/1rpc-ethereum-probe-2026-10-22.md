# 1RPC Ethereum endpoint probe - 2026-10-22

This is a bounded, read-only candidate check. It does not approve 1RPC for
external product use or establish reliable chain indexing.

## Requests and observations

At 2026-10-22 04:58:29-04:58:34 UTC, five sequential JSON-RPC POST requests
were sent through the existing Windows HTTP proxy (`127.0.0.1:7897`) to the
documented public endpoint `https://public.1rpc.io/eth`. No account, key,
transaction, signing operation, or write method was used. JCT is the verified
Ethereum contract `0xc477b6dfd26ec2460b3b92de18837fd476ea7549`.

| Method | Request parameters | HTTP / JSON-RPC result | Response SHA-256 (UTF-8 body) |
| --- | --- | --- | --- |
| `eth_chainId` | `[]` | HTTP 200, `0x1` | `393E6087ADFD530B42AA8DE473D0CFC36836B47861C06D285112805782069AD2` |
| `eth_blockNumber` | `[]` | HTTP 200, `0x1907b1a` at 04:58:31 UTC | `9E6EEA902F197E871C1C36649A880896440FDA8AE72FDFBCF94EEE8DAE69BD31` |
| `eth_call` JCT `decimals()` (`0x313ce567`) | contract call pinned to block `0x1907b00` | HTTP 503; the PowerShell client exposed no response body | unavailable |
| `eth_call` JCT `totalSupply()` (`0x18160ddd`) | contract call pinned to block `0x1907b00` | HTTP 410; the PowerShell client exposed no response body | unavailable |
| `eth_getLogs` JCT `Transfer` topic | `fromBlock=0x19079ef`, `toBlock=0x1907b00` | HTTP 200, JSON-RPC `-32602`: `eth_getLogs is limited to 0 - 50 blocks range` | `7F55996675EDB3EEE641577D5151A17F0793D74AD3F8B47E1066F1B7914692B7` |

Successful response bodies were exactly:

```json
{"jsonrpc":"2.0","result":"0x1","id":1}
{"jsonrpc":"2.0","result":"0x1907b1a","id":1}
{"jsonrpc":"2.0","error":{"code":-32602,"message":"eth_getLogs is limited to 0 - 50 blocks range"},"id":1}
```

The request bodies used JSON-RPC 2.0, ID 1, the method and parameters shown
above. The 100+ block log request was rejected by the provider's stated range
limit; no logs were returned. Because the pinned contract calls failed, this
run did not validate JCT decimals or supply against the fixed block and did not
produce any JCT transfer data.

## Official documentation and use decision

The official 1RPC getting-started page documents public endpoints in the form
`https://public.1rpc.io/<network>` and shows Ethereum at `/eth`. Its Plans page
describes a Starter plan capped at 1,000 requests per day and says the plan
supports public endpoints. The public endpoint worked without a key in this
probe. The reviewed documentation describes relay/privacy behavior and plans,
but did not establish rights to display, retain, cache, redistribute, or derive
metrics from chain responses in this external-user product. A working public
endpoint is not an external-use license.

Decision: technically reachable for chain ID and head height, but rejected for
the current zero-budget external product pending explicit use terms and stable
contract-call/log coverage. Keep disabled. It cannot supply a market directory,
prices, holders/Top30, exchange derivatives, liquidations, entity labels, or
event-time prices. The endpoint's per-request 50-block log ceiling would also
require pagination and measured rate/quota before any history design.

Official references:

- https://1rpc.io/
- https://docs.1rpc.io/using-the-web3-api/getting-started
- https://docs.1rpc.io/using-the-web3-api/networks
- https://docs.1rpc.io/using-the-web3-api/payment

No application adapter or database record was changed by this probe.
