# Source integration test baseline - 2026-10-22

## Run

The full Maven test suite was run from the workspace with its bundled Maven
3.9.11 and JDK 21.0.12.1. The test database was MySQL 8.4.13 at
`127.0.0.1:3307/observatory_test`; Flyway validated all nine migrations. No
live provider credentials were configured. `AcquisitionProbeLiveTest` was
skipped by its test guard, so this run is not evidence of a fresh external
provider response or source-to-API reconciliation.

## Result

The suite executed 81 tests: 79 passed, one failed, and one was skipped. The
failure is
`ExternalSecurityTest.registrationSupportsTheContractMaximumPasswordWithoutTruncation`:
it expected HTTP 201 when registering a 128-character ASCII password, but the
application returned HTTP 400 with `password cannot be more than 72 bytes`.
The application currently uses `BCryptPasswordEncoder`, whose input limit is
the relevant boundary. This is an account/security contract mismatch, not a
source-ingestion test failure; it remains unresolved in this source-focused
work increment.

All executed source-related checks passed, including acquisition evidence
parsing, asset mapping, metrics, provider mapping, HTTP client behavior,
transfer validation, and OpenAPI contract validation. The existing contracts
continue to describe the seven-entry configured catalog as `partial`; they do
not prove the configured catalog matches CoinAnk's market universe.

## Production evidence still missing

This test run does not validate a provider with current credentials, external
display rights, live directory coverage, exact JCT market/chain alignment,
or real-data passage through a production-approved adapter. Those gates remain
open in `source-matrix.md`. The active preview on port 8080 was not restarted;
the earlier runtime recheck documents that it may be stale relative to source.

