# External account lifecycle

Status: SDD contract for external-mode provisioning. Public self-registration is
enabled. Accounts belong to the service, not to a provider or wallet.

## Bootstrap and roles

- `APP_USER` and `APP_PASSWORD` identify the bootstrap administrator. External
  mode requires a password of at least 16 characters and secure cookies as
  specified in `deployment.md`.
- On startup, the bootstrap account is created or reconciled to the configured
  password, enabled state and `ADMIN` role. This is the recovery path if the
  administrator loses access. Store these values in the deployment secret
  manager and rotate the configured password with the deployment change.
- Anyone may register an enabled `RESEARCHER` account with a unique username and
  password. The API never returns password hashes. A disabled
  account cannot authenticate. Account rows are retained so existing user data
  ownership remains stable.
- Administrators may also create, disable/re-enable, and reset passwords for
  `RESEARCHER` accounts. Public registration cannot select a role.
- Administrators cannot disable their own account or the final enabled
  administrator. Role assignment is not available through the public API.
- Registration requires only the session-bound CSRF token from public
  `GET /api/csrf`; administrator writes require valid HTTP Basic credentials
  and that same session-bound token. Account deletion and role escalation are
  not provided.

## API contract

| Method and path | Access | Behavior |
| --- | --- | --- |
| `GET /api/csrf` | Anonymous | Issue session cookie and CSRF token for registration |
| `POST /api/accounts` | Anonymous + CSRF | Register enabled `RESEARCHER`; username `[A-Za-z0-9._@-]{3,64}`, password 16-128 characters |
| `GET /api/admin/accounts` | `ADMIN` | List username, role, enabled and creation time only |
| `POST /api/admin/accounts` | `ADMIN` + CSRF | Create an enabled `RESEARCHER`; username `[A-Za-z0-9._@-]{3,64}`, password 16-128 characters |
| `PUT /api/admin/accounts/{username}/password` | `ADMIN` + CSRF | Replace the BCrypt password using the same length rules |
| `PUT /api/admin/accounts/{username}/enabled` | `ADMIN` + CSRF | Set enabled state; reject self-disable and last-admin disable |

Data ownership is the authenticated username. Provisioning an account does not
copy or expose another user's watches, notes or table preferences. This lifecycle
does not by itself provide TLS, account recovery email, MFA, registration rate
limits, email verification, bot controls, abuse monitoring or a public release
perimeter. These are required deployment/security gates before opening
registration to the public internet.

## Acceptance

- MySQL persists accounts and role/enabled state across process restart.
- Unauthorized and `RESEARCHER` callers cannot list or mutate accounts.
- New account credentials work; disabled accounts fail authentication; password
  reset invalidates the prior password.
- Responses omit password hashes, duplicate usernames conflict, and ownership
  remains isolated.
- Anonymous visitors can load the app and register only a `RESEARCHER`; they
  cannot read market/workspace APIs before authentication.
