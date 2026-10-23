# Runtime recovery recheck — 2026-10-22

## Active preview

- PID 9564 remained bound to `127.0.0.1:8080`; it was not stopped or restarted
  to preserve the user's active page.
- `/actuator/health` returned HTTP 200 with `status=UP`.
- `/api/sources` returned six candidate records; all six had `enabled=false` and
  `productionApproved=false`.
- `/api/markets/query?limit=2` returned no bytes within 10 seconds.
- `.tools/app-8080.error.log` contains `NoClassDefFoundError` for
  `ch/qos/logback/classic/spi/ThrowableProxy` during request handling.
- `target/asset-observatory-0.1.0.jar` is a 1,548,444-byte plain JAR with no
  `BOOT-INF/lib` dependencies. An earlier repackaging attempt failed to rename
  it to `.original` because the active process has it open; the process may
  still serve already-loaded classes while lazy class loading fails.

## Isolated build

The current `pom.xml`, `src`, and built `frontend/dist` were copied to
`.tools/sourcecheck-build/`, then packaged there with the pinned Maven 3.9.11
and JDK 21. The package command completed successfully and produced a separate
33,783,342-byte JAR at
`.tools/sourcecheck-build/target/asset-observatory-0.1.0.jar`. It did not
replace the active target JAR or restart port 8080. This confirms packaging,
not successful runtime startup or source-data availability.

## Test result

The current full Maven suite ran 81 tests: 80 passed, one opt-in live probe was
skipped, and one failed. The failure is
`ExternalSecurityTest.registrationSupportsTheContractMaximumPasswordWithoutTruncation`:
the API contract maximum password is rejected at 72 bytes by the password
encoder. Source mapping, evidence replay, and OpenAPI contract tests passed in a
separate targeted run. This account test is outside the data-source change but
remains a backend regression.

The local preview was not restarted; its recovery remains pending until the
user's active page session can be coordinated.
