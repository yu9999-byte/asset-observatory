# Deployment and recovery contract

This document is the SDD contract for a repeatable container deployment. It does
not grant market or chain data redistribution rights and does not enable any
provider. `APP_SOURCES_ENABLED` is deliberately forced to `false` in the sample
Compose file until the source matrix gates are independently unlocked.

## Required inputs

- MySQL 8.4-compatible server and a dedicated database/user.
- `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD` supplied through the deployment
  secret store, never committed to an environment file.
- `APP_USER` and an `APP_PASSWORD` of at least 16 characters.
- TLS termination in front of the app. The sample binds the host port to
  loopback; an operator may expose it only behind an authenticated reverse proxy.

`APP_USER` is the bootstrap administrator. On each external-mode startup the
application reconciles that account to the configured password, enabled state,
and `ADMIN` role, so the secret must be rotated through the deployment secret
store. Anyone can self-register a `RESEARCHER` account using the flow in
`docs/account-management.md`; market, workspace and account administration APIs
remain authenticated. Account passwords must be at least 16 characters. Do not
use a shared public/demo credential. Public registration has no rate limiting,
email verification or bot controls yet, so it is not approved for unrestricted
internet exposure until those controls and an abuse-response plan are deployed.

## Contract

`docker compose up -d --build` starts MySQL, waits for its health check, and
starts the Java service. Spring/Flyway owns schema migration. The app must start
with `APP_EXTERNAL=true`, `APP_BIND=0.0.0.0`, `APP_SECURE_COOKIE=true`, and the
source approval flags disabled. A missing or weak credential fails startup rather
than silently creating an external unauthenticated service.

The only readiness signal is `GET /actuator/health` returning `{"status":"UP"}`.
The operator must additionally verify authenticated `GET /api/sources`, the
database migration version, and a read/write workspace operation before routing
traffic. No provider response is part of the readiness check.
For external access, separately verify public registration, researcher login,
confirm that researcher access is denied on admin routes, and verify account
disable and password reset behavior before admitting users.

## Backup and restore runbook

1. Stop or drain app traffic and set `DATABASE_PASSWORD` in the process
   environment.
2. Run `./scripts/backup-mysql.ps1 -Database observatory`; retain the SQL file
   and adjacent SHA-256 JSON in protected backup storage.
3. Verify the SHA-256 before restore. Stop the app, create an isolated target
   database, then run `./scripts/restore-mysql.ps1 -DumpPath <file> -Database
   <target> -ConfirmRestore`.
4. Start the app against the target, verify Flyway, health, authenticated API,
   and a canonical workspace response. Keep the original database untouched
   until the checks pass.

The restore script requires an explicit switch because it overwrites data. It
uses `MYSQL_PWD` only for the child process and removes it immediately; passwords
are not placed in process arguments or evidence logs.

## Acceptance evidence

The local `verify-local-restore.ps1` evidence proves a nonempty MySQL dump/restore
with exact hashes for this workspace. The container contract remains open until a
deployment environment runs the same steps, including app crash/restart,
database restart, backup verification, and rollback to the previous image.
