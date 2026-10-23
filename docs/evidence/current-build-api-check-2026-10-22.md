# Current-build API check - 2026-10-22

## Method

Started the current Java source tree on isolated port `8083`, using the local
`observatory_test` MySQL database. The existing preview on port `8080` was not
restarted or modified. After the read-only API checks, the temporary process
was stopped.

## Observation

`GET /api/markets/query?limit=10` returned `total=7` and
`completeness="partial"`. The response contained zero populated market
measurements across the configured rows; `fetchedAt` was null. This is an
incomplete configured catalog, not the full market universe and not live market
coverage.

`GET /api/sources` included `DeBank Cloud` with `enabled=false` and
`productionApproved=false`. Its gap states that address history requires an
AccessKey, the free trial is limited, and permission for external display,
storage, redistribution, and derived metrics has not been confirmed.

The current build therefore fixes the misleading `complete` status observed in
the older port-8080 preview, but does not resolve the primary data-access and
external-use blockers. This check is a local API behavior check, not a real
provider-to-database-to-API reconciliation.

## Verification

- `AcquisitionEvidenceTest` and `OpenApiContractTest`: 12 tests passed.
- Current-build API check on port `8083`: catalog was `partial`; DeBank stayed
  disabled and unapproved.
