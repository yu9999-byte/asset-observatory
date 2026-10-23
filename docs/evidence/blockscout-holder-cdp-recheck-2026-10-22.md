# Blockscout JCT holder CDP transport recheck - 2026-10-22

## Scope and result

One read-only navigation was attempted through the configured web-access Chrome
CDP proxy at approximately 2026-10-22 05:00 UTC for the documented public
Blockscout Ethereum API:

`GET https://eth.blockscout.com/api/v2/tokens/0xc477b6dfd26ec2460b3b92de18837fd476ea7549/holders`

The CDP `/new` call created a background target. Its first `/info` and `/eval`
observations still reported `about:blank`. A subsequent `/navigate` call did
not return within 10 seconds; `/info` then timed out after 4 seconds without
an HTTP response, status code, or response body. The temporary target was
closed successfully. No user tab was opened, navigated, or closed.

## Interpretation

This is a workstation browser/CDP transport failure for this attempt. It is not
an HTTP response from Blockscout, an empty holder result, evidence of provider
outage, or a licensing decision. No holder rows, count, block height, timestamp,
quota, or freshness information was observed. The existing 2026-10-21 browser
samples for other contracts and the 2026-10-22 Java `ConnectException` checks
are not refreshed by this attempt.

Blockscout remains disabled. Its previously reviewed production/API migration
and external-use rights gaps remain. Do not retry this same endpoint through
the same CDP path unless the host transport changes; any future eligible
recheck needs a reachable application/deployment transport and explicit
external display, retention/cache, redistribution, and derived-use terms.
