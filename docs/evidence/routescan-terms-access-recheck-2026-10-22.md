# Routescan terms access recheck - 2026-10-22

At 2026-10-22 04:32 UTC, a bounded read-only request through the machine's
existing Windows system proxy to the official URL
`https://routescan.io/terms-and-conditions` returned an Access Denied page
with HTTP 403. The captured 7,883-byte body is
`evidence/acquisition/routescan-terms-20261022/terms-response.html`, SHA-256
`E3ACCD967766D151FAAE1608915ED9802FAEDDA7334A92C894AB29AFC5709123`. Its
message says the client does not have permission to access the resource and
suggests checking the site status page. This is an access restriction on the
current path; it is not evidence of Routescan outage or its licensing policy.

The official documentation root `https://docs.routescan.io/` returned HTTP 200
but only the JavaScript-required shell, so no API quota or usage policy text was
read. No Routescan API endpoint was called in this recheck. Earlier development
evidence remains limited to four matching Ethereum JCT transfer rows, without
`log_index`; it does not establish complete coverage, continuity, event
identity, or external-use rights.

Decision: leave Routescan disabled. External display, storage/cache,
redistribution, derived-metric permission, quota and production availability
remain unverified. Unlock requires official terms accessible for review or
written provider clarification plus an independently verified event identity
contract.
