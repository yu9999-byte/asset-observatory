# Java data acquisition evidence — 2026-10-22

This is a local acquisition milestone, not system completion or production
approval. The user authorized source verification before choosing the solution,
with external users, zero initial data budget and MySQL only when storage is
needed. No provider content is installed as runtime fallback data.

## Reproducible runs

Run `./scripts/verify-data-acquisition.ps1 -Scope all -Transport Direct` for the
bounded candidate set, or `-Scope core` for indexed-transfer/raw-log matching.
These opt-in probes use Java21.0.12.1, actual application HttpClient construction,
TLS verification, GET-only targets, no redirects, a 10s request budget, bounded
bodies and sequential pacing. Reports retain exact URLs, UTC times, status/root
errors, sizes, SHA256 and raw response files. A transport timeout may exceed its
nominal deadline by scheduling overhead; this is not a production SLA test.

| Run | Shanghai observation window | Result | Evidence |
| --- | --- | --- | --- |
| all/direct | 2026-10-22 00:22:05–00:23:20 | 4 of16 requests captured HTTP200;12 transport failures | `acquisition/20261021T162159001Z-direct/report.json` |
| core/direct, dependent block queries | 2026-10-22 00:25:45–00:26:07 | 6 of7 requests captured HTTP200;1 holder connection failure | `acquisition/20261021T162534395Z-direct/report.json` |

Windows system proxy was disabled. The System-transport wrapper refused to use
inactive saved settings; no network configuration was changed. These are two
dated observations, not sustained operation or a claim that a provider is
globally unavailable.

## What the real responses prove

1. CoinLore returned seven directory assets: BTC, ETH, LINK, UNI, USDC, WBTC and
   Janction/JCT ID183005. The parser checks ID/name/symbol and exact decimals.
   JCT price/changes/USD turnover are parseable; `market_cap_usd` and `csupply`
   contain invalid `0?`, while total supply is absent. These remain null. The
   response does not supply a quote snapshot time; observation time cannot prove
   freshness. The provider ID's link to the verified Ethereum contract remains
   open. The17 venue records are discovery clues, not perpetual-contract proof.
2. Routescan returned four historical transfers for one specified JCT Ethereum
   address. Four bounded block queries returned seven canonical ERC-20 logs.
   Each indexed transfer has one exact log match for transaction/block hash,
   height, contract, from/to, raw integer amount, decimals and timestamp. Exact
   identity is chain + transaction hash + logIndex, never transactionIndex.
   Burn logs remain distinct. `tokentx` lacks logIndex; `getLogs` supplies it.
   Thus the missing-index issue is resolved for this sample, while independent
   reconciliation, pagination completeness and finality remain unverified.
3. The sampled transfer times range from2025-11-07T12:33:47Z to
   2026-05-16T13:25:11Z. They do not establish present balances, a complete 24H
   flow or live monitoring. No event-time price was obtained.
4. Blockscout token/holders, Ethplorer token/holders, MEXC contracts, both DEX
   alternatives and Kraken Futures failed with connection errors. Bitget coin
   and contract directories and Kraken spot failed with socket errors. Gate
   contracts timed out. These failures have no HTTP success or schema evidence.
   Blockscout holders failed again in the bounded reconciliation run.

`AcquisitionEvidenceTest` checks the retained core raw-body hashes and repeats
normalization and all four matches offline. `DiscoveryMappingTest` covers
invalid/missing/zero fields, foreign identities, malformed logs and uint256
precision. These are complementary to, not substitutes for, live probes.

## Official usage references and review limits

The following official references were reviewed in this acquisition session.
These notes record identified gates, not legal approval or an inference that
public HTTP access grants third-party distribution.

| Source | Official reference | Remaining gate |
| --- | --- | --- |
| CoinLore | https://www.coinlore.com/cryptocurrency-data-api ; https://www.coinlore.com/terms-of-use | API presentation vs general content-use terms need clarification for external display, storage and derived research outputs |
| Routescan | https://docs.routescan.io/ ; https://routescan.io/terms-and-conditions | API plan/internal-use and content restrictions; no external-use/storage grant verified |
| Blockscout | https://docs.blockscout.com/devs/pro-api ; https://eaas.blockscout.com/terms-and-conditions | Separate unified product needs account/key, quota and applicable external-use scope; legacy connectivity is not PRO validation |
| Bitget | https://www.bitget.com/support/articles/12560603852058 | Data licensing includes written-authorization requirements for commercial/third-party distribution |
| CoinGecko alternative | https://www.coingecko.com/en/api_terms | Personal/noncommercial and plan-specific terms do not establish entitlement for this external research product; competing-product limits require review |

No accounts were registered, plans purchased or permission requests sent.
External access is not automatically equivalent to commercial use, but it does
require the applicable third-party display/storage rights to be established.

## Decision and unlock conditions

| Gate | What is missing | Concrete unlock / next acceptance |
| --- | --- | --- |
| G01 identity | CoinLore ID and JCT venues not tied to chain/contract | Official provider/issuer deployment and venue-instrument evidence; retain chain-specific mapping |
| G02 current chain coverage | No current Java-reachable eligible holder index; earlier29/30 discrepancy unresolved | Eligible reachable index/key or complete fixed-block reconstruction; compare quantities/denominator/time and pagination independently |
| G03 external rights | No verified external display/storage entitlement for selected market/index sources | Document applicable free entitlement or operator-provided authorization; configure policy scope/retention before enabling |
| G04 derivatives | No live JCT instrument/unit/funding/OI/liquidation evidence | Accessible official venue specification and samples; reconcile units, timestamps, instruments and bounded coverage |
| G05 historical metrics | 1H–1Y balances, event-time prices and complete flow windows absent | Permitted backfill or accrue approved snapshots; expose coverage start/gaps; do not invent prior history |
| G06 production | Stable network, managed accounts/TLS/deployment and enabled-source recovery unproven | Explicit deployment environment plus load, outage/backfill/reorg and restore evidence; no public release in this task |

Acquisition determines the path: backend HTTPS adapters for sources, bounded
in-memory cache for approved live queries, MySQL for user records and only
required permitted history/checkpoints. Redis is optional, not a prerequisite.

The original three-page flow, rankings, full table behavior, watch persistence,
floating notes and all R01–R24 remain required. This evidence does not silently
reduce scope. Next runnable increments are tracked in `../execution-plan.md`;
source-dependent production work remains gated by G01–G06.

## Implemented and verified local increment

`GET /api/sources` now exposes curated verification time, transport result,
bounded scope, rights status and evidence ID independently from `enabled` and
`productionApproved`. The light source page displays these records without raw
market samples. CoinLore and Routescan remain disabled, as do the prior sources.
Preview8080 and isolated test8081 were rebuilt/restarted; MySQL was not stopped.

- Maven ordinary suite:53 passed,0 failures/errors,1 opt-in live test skipped.
  The separate live runs above have their own reports. Surefire XML is at
  project-relative `target/surefire-reports/`.
- Frontend:12 unit tests passed; `npm run build` passed TypeScript and Vite.
- Browser:10 Playwright cases passed across desktop1440x1000 and iPhone13,
  including source review, three-page missing-state navigation, persistent
  notes/watches and layout conflicts. The source-page screenshots were visually
  checked and preserved under `acquisition-ui/`.

Tests were not taken to establish licensing, fresh market data or production
readiness. The main research pages still have missing source data; no recorded
probe body is loaded into production APIs. Prior load/restore/restart evidence
is retained separately and is not relabeled as a new production drill.

## CoinGecko browser-route recheck - 2026-10-22 02:46 UTC

The verified Ethereum JCT contract endpoint was opened once in a new Chrome
background tab through the web-access CDP proxy after direct HTTPS probes had
timed out. Chrome returned `chrome-error://chromewebdata/`; no HTTP status,
response body, schema or source fields were available. The temporary tab was
closed and existing user tabs were left untouched. This indicates the current
host/browser route could not reach the endpoint; it does not establish provider
absence or outage. Terms were not reviewed because no service response was
obtained. Evidence: `acquisition/coingecko-cdp-20261022/report.json`. Do not
enable the source. Recheck when an authorized network route is available.
