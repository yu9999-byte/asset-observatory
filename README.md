# Asset Observatory

Java 21 / Spring Boot, React / TypeScript, MySQL. Work in progress; no production
release or live-data acceptance is claimed. Full scope and remaining gates are in
`docs/specification.md` and `docs/traceability.md`.

The machine-readable API contract is `docs/openapi.json` (OpenAPI 3.1.0).
Maven validates route coverage and real responses against it, including external
authentication and CSRF regressions. The current Java suite has 40 passing tests,
including integration tests against isolated MySQL. The policy-gated holder-page
journal in `docs/data-model.md` is implemented through migrations V4-V5, with
atomic checkpoints, exact quantities and immutable policy evidence. No production
policy or worker is enabled; broader history and historical coverage remain open.

## Local preview

From the project directory in PowerShell:

```powershell
./scripts/start-local-app.ps1
```

The script starts the isolated MySQL instance on `127.0.0.1:3307` and the built
application on a free loopback port in 8080-8090. The resulting URL is recorded in
`.tools/local-app.json`. Public sources are disabled; the UI shows missing data.
The development database credentials and passwordless local root are exclusively
for this isolated development instance and must be replaced for deployment.
Windows native MySQL uses a verified ASCII directory junction to this workspace;
the actual database files remain under the project `.tools` directory.

## Build and verification

Bundled Java and Maven must already be installed in `.tools`.

```powershell
cd frontend
npm ci
npm test
npm run build
cd ..
$env:JAVA_HOME = Join-Path (Get-Location) '.tools/jdk-21.0.12.1+1'
./scripts/start-local-mysql.ps1
& .tools/apache-maven-3.9.11/bin/mvn.cmd -s scripts/maven-settings.xml verify
```

Stop this project's running application processes before packaging on Windows,
where running jars are locked. Java integration tests use `observatory_test`,
not the preview database. Override `TEST_DATABASE_URL`, `TEST_DATABASE_USER` and
`TEST_DATABASE_PASSWORD` for another isolated MySQL test instance.

Playwright requires installed Chrome and an app bound to `observatory_test` on
port 8081; its default base URL is that test instance. Do not target a personal
workspace: tests change LINK/JCT notes, a LINK watch and research table preferences.

```powershell
./scripts/start-local-app.ps1 -Database observatory_test -PreferredPort 8081
cd frontend
npm run e2e
cd ..
./scripts/verify-local-restart.ps1
node scripts/verify-local-load.mjs
```

If 8081 is busy, use the test URL recorded in `.tools/test-app.json` for
`E2E_BASE_URL`. The current E2E proves missing-source navigation, workspace
persistence and table layout recovery/conflicts. The restart drill terminates
only the verified managed test app, restarts it and compares persisted state.
The load measurement runs 20 readers for 60 seconds against the test instance;
it requires sources disabled and an existing saved market layout. Evidence is
written to `docs/evidence/local-{restart,load}-verification.json`. These local
checks do not establish real-data acceptance or production capacity.

```powershell
./scripts/verify-local-restore.ps1
```

The restore drill uses only `observatory_test`, writes nonempty workspace and
holder-journal fixtures, and restores into a new database. It compares complete
dump hashes and verifies exact uint256 strings, NULL timestamps, microseconds,
checkpoint/version and immutable policy JSON. The fixture policy is disabled;
fixture hashes are synthetic. It preserves its backup and evidence for review.
This does not replace worker replay, production deployment, rollback or recovery.

## Container deployment contract

`Dockerfile`, `docker-compose.yml` and `docs/deployment.md` define the reviewed
container shape. Compose requires explicit database and external-user secrets,
starts with all providers disabled, and binds the host port to loopback until a
TLS reverse proxy is configured. Use `scripts/backup-mysql.ps1` and
`scripts/restore-mysql.ps1 -ConfirmRestore` for the documented backup flow.
Docker runtime/recovery was not available on the verification host, so these
artifacts are not production acceptance evidence.
