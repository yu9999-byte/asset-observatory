# Etherscan transfer API candidate probe - 2026-10-22

## Scope and result

This was a bounded, read-only check through the workstation's existing system
proxy on 2026-10-22. No account was registered and no API key was supplied.
It tests whether Etherscan can close the Ethereum JCT transfer-event source gap
for the externally accessible research product.

The official API documentation page
`https://docs.etherscan.io/api-endpoints/accounts` returned HTTP 200 (263,243
bytes; SHA-256
`287F1EAC4CD172253EE163A921C18E69C46CB16CF3D41A637EA38B84CB0A92FF`). Its
content advertises on-chain endpoints including token transfers and a unified
API across 60+ EVM chains. The official rate-limit page
`https://docs.etherscan.io/resources/rate-limits` returned HTTP 200 (242,022
bytes; SHA-256
`93E0F4377E73C2B1111C14861658D50EC7DB6299B9F6572FB08AC0E859DBDCE2`). This
probe did not establish an applicable free quota or production service level.

One request was made to the documented V2 account token-transfer endpoint for
Ethereum JCT (`chainid=1`, contract
`0xc477b6dfd26ec2460b3b92de18837fd476ea7549`, one requested row). The request
omitted the API key. It returned HTTP 200 with the exact 67-byte JSON body
`{"status":"0","message":"NOTOK","result":"Missing/Invalid API Key"}`;
body SHA-256:
`15A829FEAAC786F421A396F1E60977A508A6E29434BD7B62A75A743710CDB150`. No
transfer records were returned. This demonstrates the tested endpoint requires
a valid API key for this request; it does not establish that keyed access is
free or externally licensed.

The official URL `https://etherscan.io/terms-of-service` returned HTTP 200 but
served an Etherscan Error Page HTML document, not readable terms (41,195 bytes;
SHA-256
`39135DC0ADFF1C2044F997DBEE3E2BF2E739A5F95E6F806CF25694656CEBF01D`). This
is a page-access/content failure on the tested route, not a licensing decision.

## Decision

Keep Etherscan disabled. The probe did not validate usable response fields,
historical completeness, pagination continuity, event identity, holder
coverage, price-at-event data, refresh quota, or deployment-host reachability.
External display, storage/cache, redistribution and derived-metric rights are
unknown. Do not infer any of these from the fact that documentation and an API
host are publicly reachable.

Unlock requires a provider-approved API key/plan and terms or written
clarification covering the intended external product, data display, storage,
cache, redistribution and derived metrics. Then repeat the exact-address
request with a valid key, preserve the raw response, and reconcile event IDs
against an independently verified chain source before considering an adapter.
