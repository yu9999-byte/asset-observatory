# CoinPaprika market-directory alternative probe — 2026-10-22

This is a bounded transport check for an alternative asset-directory candidate.
It did not reach the provider documentation and establishes neither provider
availability nor data or licensing terms.

## Attempt

- Intended source: official terms page at
  `https://docs.coinpaprika.com/terms-and-conditions`.
- Browser route: web-access Chrome background tab, created for this probe.
  The tab resolved to `chrome-error://chromewebdata/`; no HTTP status, response
  body, or page fields were available.
- Reader route: Jina Reader request to the same official URL. `curl.exe`
  returned exit code 1 after 15.5 seconds with no response body. No HTTP status
  or body hash was captured.
- Search discovery was unavailable because the `agent-reach` command and
  configured Exa MCP tool are not installed in this host. A Chrome Google
  search navigation also ended at `chrome-error://chromewebdata/`.
- The temporary browser tabs were closed. Existing user tabs were not used or
  changed.

## Decision

CoinPaprika remains an unverified candidate. This probe observed no API schema,
market count, asset identifiers, timestamps, pagination, quota, cost, or terms
for public display, storage, caching, redistribution, and derived metrics. It
cannot currently replace the CoinAnk comparison directory or any runtime
source. The failure is classified as a host/browser and reader transport
failure, not a provider outage or empty dataset.

## Unlock

Retry one official documentation and terms read when an authorized network
route can reach the provider. Only then decide whether a bounded live response
is justified and whether its identifier/scope and external-use conditions can
meet the product requirements. No source was enabled and no runtime data was
changed by this probe.
