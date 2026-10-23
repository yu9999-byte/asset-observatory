# Execution environment readiness — 2026-10-22

Checked at 2026-10-22 02:20 UTC while continuing the approved acquisition-first
plan. This records the current Codex execution environment; it does not replace
earlier Java acquisition reports or imply anything about provider uptime.

## Results

| Check | Result | Meaning |
| --- | --- | --- |
| Java / Maven | Project-pinned JDK 21 and Maven 3.9.11 are present under `.tools`; their executables are not added to `PATH` | Use the repository tool paths, as documented in `README.md`; no system installation is needed. |
| Agent Reach | `agent-reach doctor --json` failed because the command is not installed | The configured internet-research routing tool is unavailable in this execution environment. `check-update` could not be run for the same reason. |
| Web-access dependencies | Node 25.8.2; Chrome debugging port 9222 and the local CDP proxy are available | Dependency check only; no browser tab or user page was used for this check. |
| Docker | `docker` is not discoverable on `PATH` | Container build, Compose, and container recovery remain untestable here. |
| Application preview | `127.0.0.1:8080` and `:8081` both returned HTTP 200 from `/actuator/health` | Local software is running; this does not prove a production deployment. |
| Source register | `GET /api/sources` returned 6 records; 0 enabled and 0 production-approved | No real provider data is configured for runtime display. Per-source transport observations are not activation or entitlement. |

An initial attempt used bare `mvn` and failed because Maven is not on `PATH`.
The project-pinned toolchain was then located under `.tools`; after setting
`JAVA_HOME` and using the documented executable path, the targeted probe ran.
Its three host requests ended in `ConnectException` with no HTTP status or body.
Full request timing and interpretation are in
`evidence/holder-source-recheck-2026-10-22.md`.

## Resume conditions

- Restore `agent-reach` if internet search is needed; otherwise keep using only
  verified, documented read-only provider endpoints through the approved
  acquisition workflow.
- Provide a Docker runtime and an explicitly designated deployment database
  before container, rollback, and crash-recovery acceptance.

Until these conditions are met, source freshness and deployment recovery gates
remain open. The health checks and empty source register are recorded only as
local readiness observations.
