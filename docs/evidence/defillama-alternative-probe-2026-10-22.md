# DeFiLlama market-data alternative probe — 2026-10-22

This bounded check did not reach DeFiLlama documentation and establishes
neither API availability nor data-use rights.

## Attempt

- Intended source: official API documentation at
  `https://defillama.com/docs/api`.
- Browser route: `web-access` Chrome/CDP dependency check passed. A new
  background tab was used; navigation returned `net::ERR_CONNECTION_TIMED_OUT`
  and the page resolved to `chrome-error://chromewebdata/` without a provider
  HTTP response or body. The temporary tab was closed; existing tabs were not
  changed.
- Reader route: one request to
  `https://r.jina.ai/https://defillama.com/docs/api` ended after 15.0 seconds
  with curl exit code 28, HTTP status 000, and zero response bytes.
- `agent-reach doctor --json` remains unavailable because `agent-reach` is not
  installed in this host, and no Exa MCP search tool is configured.

## Decision

DeFiLlama remains an unverified candidate. No API schema, asset-universe
coverage, provider timestamps, historical range, quota, price, attribution, or
terms for external display, storage, caching, redistribution, and derived
metrics were observed. Do not enable it or count it toward the CoinAnk market
universe. The failure is a host/browser and reader transport failure, not
evidence of provider outage or absent data.

## Unlock

Retry one official documentation and terms read when an authorized network
route can reach the provider. Only then evaluate a bounded response and decide
whether its coverage and external-use conditions fit the product. No source
was enabled and no runtime data was changed by this probe.
