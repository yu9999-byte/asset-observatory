# Local market completeness runtime recheck — 2026-10-22

## Observation

At approximately 2026-10-22 03:53 UTC, a read-only request to
`http://127.0.0.1:8080/api/markets/query?limit=15` returned:

- `total: 7`, `completeness: complete` and a new `snapshotAt`.
- All seven configured catalog rows had null market measurements and null
  `sourceAt`.
- The same response's coverage stated: `Configured seven-asset directory;
  this is not the market universe.`
- Its reason stated that measurements were unavailable and missing fields
  remained null.

The `complete` value conflicts with the API contract, which requires `partial`
until the configured directory is reconciled to the target market universe.

## Build/runtime comparison

The process listening on port 8080 was PID 9564, running
`target/asset-observatory-0.1.0.jar`, started at 2026-10-22 10:33:57 Asia/Shanghai.
That jar was last modified at 10:33:44. The current `ResearchService.java` and
compiled class were modified at 11:36:51 and 11:37:23 respectively. The current
source returns `completeness=partial`; `OpenApiContractTest` also asserts this
and passed in the Maven suite at 11:49 Asia/Shanghai.

This identifies the observed mismatch as a stale local runtime artifact, not a
current source-code result. The listening process was left running so it would
not disturb the user's active page session. Rebuild/restart the local preview
from the current source before treating the live 8080 API as current.

## Decision

The production/source contract remains `partial` for the seven-entry directory.
The live 8080 process must not be used as evidence that the current source marks
the directory partial until it is rebuilt. No source was enabled and no data
was changed by this check.
