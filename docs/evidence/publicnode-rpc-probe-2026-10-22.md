# PublicNode RPC verification - 2026-10-22

This is a bounded, read-only connectivity and terms check. It does not approve
PublicNode for the external product or establish chain-index completeness.

## Observed requests

Observation window: 2026-10-22 04:18 UTC. Requests used the existing Windows
system proxy at `127.0.0.1:7897`, HTTPS certificate validation, and JSON-RPC
read methods only. No account, credential, signing key, transaction, or write
method was used.

| Endpoint and method | Response | What it establishes |
| --- | --- | --- |
| `https://ethereum-rpc.publicnode.com`, `eth_chainId` | HTTP 200, `0x1` | Ethereum mainnet endpoint answers from this host through the configured proxy. |
| Ethereum, `eth_blockNumber` | HTTP 200, `0x1907a49` at 04:18 UTC; follow-up observed `0x1907a53` | A recent chain head was returned. This is not an independently verified freshness or finality SLA. |
| Ethereum, `eth_call` to JCT `0xc477b6dfd26ec2460b3b92de18837fd476ea7549`, selector `0x313ce567` (`decimals`) | HTTP 200, ABI result `0x12` | JCT reports 18 decimals at the sampled head, consistent with the existing identity evidence. |
| Ethereum, `eth_getLogs` for JCT `Transfer` topic from `0x19079ef` through observed head `0x1907a53` | HTTP 200, empty result | The bounded 100-block window had no matching events. It says nothing about earlier history or complete transfer coverage. |
| `https://bsc-rpc.publicnode.com`, `eth_chainId` | HTTP 200, `0x38` | BNB Smart Chain mainnet endpoint answers from this host through the configured proxy. |
| BNB Smart Chain, `eth_call` to JCT `0xea37a8de1de2d9d10772eeb569e28bfa5cb17707`, selector `0x313ce567` (`decimals`) | HTTP 200, ABI result `0x12` | BNB JCT reports 18 decimals at the sampled head. |

One initial `eth_getLogs` attempt used an invalid locally computed block range
and was rejected with `block range extends beyond current head block`; the
corrected bounded request above succeeded. One direct PowerShell request without
the proxy timed out after 12 seconds. Neither failure indicates provider data
absence.

The homepage `https://publicnode.com/` described endpoints for 78 blockchains,
called the service “free-est,” and linked its Terms of Service at
`https://publicnode.com/terms`. This is provider marketing, not a quota or SLA.

## Terms and decision

The terms page was read in a background browser tab on 2026-10-22. It describes
RPC access as a service and grants a limited, non-transferable, freely revocable
license. It also restricts copying, displaying, distributing, transmitting,
publishing, reusing, or creating derivative works from the Service or Service
Content, and prohibits scraping/data-gathering methods. The terms make no
availability or accuracy warranty. This review did not establish whether the
restrictions govern blockchain RPC result data used to operate an external
research product, or whether the intended use is expressly authorized.

Decision: technically reachable for sampled reads, but external product use,
retention, caching, and redistribution rights remain unresolved. Keep disabled.
Unlock requires written provider clarification or terms expressly covering the
product's external display and operational collection, plus a quota/availability
plan sufficient for the selected chains. Public RPC access alone is not consent.

## Java integration and coverage gaps

The application currently has no PublicNode adapter. `SourceHttp` supports GET
only and its hostname allowlist excludes `ethereum-rpc.publicnode.com` and
`bsc-rpc.publicnode.com`. The current APP_PROXY setting can configure an HTTP
proxy. Prior application-specific Java probes used direct transport and failed
on several hosts. A later bounded, one-off JDK `HttpClient` POST check through
the existing Windows proxy succeeded for Ethereum (details below). This proves
that this Java runtime can reach this RPC host through that local proxy; it does
not prove that the application's `SourceHttp`, deployment network, or a
production collection path works. No runtime source was enabled and no database
values were changed.

Standard JSON-RPC can read contract calls and bounded logs, but this probe did
not establish complete holder enumeration, a full Top30 snapshot at a fixed
block, years of logs, event-time prices, labels/multisig attribution, indexer
coverage, reorg handling, or timely ongoing service. A self-hosted archive
node/indexer would still require compute, disk, operations, and a validated
historical reconstruction design; it is not a zero-cost shortcut proven by this
check.

## Official references

- https://publicnode.com/
- https://publicnode.com/terms
- https://ethereum-rpc.publicnode.com
- https://bsc-rpc.publicnode.com

## Java JSON-RPC follow-up - 2026-10-22

At approximately 2026-10-22 04:53 UTC, a one-off JDK 21 `HttpClient` used
`ProxySelector` for `127.0.0.1:7897` and sent read-only JSON-RPC POST requests
to `https://ethereum-rpc.publicnode.com`. Both responses were HTTP 200:

| Method | Exact JSON request | Exact response body | SHA-256 of response body |
| --- | --- | --- | --- |
| `eth_blockNumber` | `{"jsonrpc":"2.0","id":1,"method":"eth_blockNumber","params":[]}` | `{"jsonrpc":"2.0","result":"0x1907b00","id":1}` | `3BD1321057A3CD8CD4DB2D6D407F3C4852C6BDCBC68C29816B80ADD83C3D2140` |
| `eth_call` JCT `totalSupply()` (`0x18160ddd`) | `{"jsonrpc":"2.0","id":2,"method":"eth_call","params":[{"to":"0xc477b6dfd26ec2460b3b92de18837fd476ea7549","data":"0x18160ddd"},"latest"]}` | `{"jsonrpc":"2.0","id":2,"result":"0x0000000000000000000000000000000000000000803aaeb36d2b21521474b516"}` | `5C33D779DA2AF0D8374D3B95D4C6ADEC72154AD4CE041BE7EDEB86E1BF0DAEE7` |

The head response corresponds to block `26,245,888` (`0x1907b00`). The raw
total-supply integer is `39685023956271443611728786710`; using the previously
verified 18 decimals derives `39685023956.271443611728786710` JCT. The RPC
response itself has no observation timestamp. The `eth_call` used the moving
`latest` tag and was not pinned to the returned block, so these two values are
not an atomic fixed-block snapshot. The requests were observations only and
were not persisted in application storage.

This advances Java transport feasibility only. It is not an application
integration or ingestion test, does not enumerate holders or historical logs,
and does not resolve PublicNode's terms, quota, freshness SLA, retention, or
external-display rights. Keep the candidate disabled until those conditions
are clarified; if approved, implement bounded POST support and pin related
calls to a block before using them for reconciled metrics.
