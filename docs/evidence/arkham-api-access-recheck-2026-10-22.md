# Arkham API access recheck - 2026-10-22

This is a bounded, read-only check of the official API landing page. It does
not test API credentials or invoke a data endpoint.

## Observation

At 2026-10-22 04:28 UTC, the web-access CDP proxy opened a new background tab
for `https://arkm.com/api`. Chrome ended at `chrome-error://chromewebdata/`
with `ERR_CONNECTION_RESET` and no HTTP status or response body from Arkham.
The tab was closed after inspection. The agent-reach executable was not
installed in this environment. Its documented Jina Reader route was attempted
once for `https://arkm.com/api` and once for `https://coinank.com/zh/openApi`;
both failed to connect to `r.jina.ai:443` after about 21 seconds, before any
reader response was received.

## Decision

These observations establish host/path transport failures only. They do not
show that either provider is down, that an API has no data, or that a provider
refuses access. No data endpoint was called, and no field coverage, timestamp,
quota, current API price, external display, storage/cache, redistribution, or
derived-metric permission was verified in this increment.

Arkham remains gated on obtaining an approved API trial/key and written terms
that cover external display and operational storage. The supplied Arkham
entity URL remains a separate identity issue: the previously verified page
identifies `2015.12.11转出200m`, one address, and does not establish a JCT token
mapping. CoinAnk remains gated on provider access to its documented paid API
and clarification of external-user display rights. Do not add an adapter or
represent either provider as a live source based on this check.
