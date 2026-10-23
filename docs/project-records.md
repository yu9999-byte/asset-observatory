# Private project records — 2026-10-22

This implements the user's original project-name, project-information and custom
note requirement as one owner-scoped research record, not a provider/entity label.
It is independent of blocked market/index source entitlement. R07/R20/R21 apply.

| ID | Contract | Required evidence |
| --- | --- | --- |
| PR01 | Store projectName, projectInfo and text separately in the existing note, keyed by authenticated owner + kind + normalized entity key | Real MySQL read/write; two owners and asset/address separation |
| PR02 | projectName <=120 UTF-16 units; projectInfo and custom text each <=4000; empty strings allowed; rendered as plain text | Boundary/Unicode/HTML tests; no inferred entity ownership |
| PR03 | Existing text/visible/version inputs remain required. Optional projectName/projectInfo may be omitted or null to preserve stored fields; explicit empty string clears them. New legacy writes default them to empty | Old-client create/update and explicit clear tests |
| PR04 | Atomically version all three fields and visibility; stale clients get409 and cannot partially modify a record; writes require existing CSRF/owner checks | Conflict, validation and isolation integration tests |
| PR05 | Note response always includes non-null projectName/projectInfo; workspace reload restores all fields | OpenAPI actual-response validation and browser reload |
| PR06 | Existing compact floating editor edits three labeled fields, previews saved values on hover, retains drafts through pin/fold, and saves/hides/reopens | Frontend failure tests before feature, desktop/mobile browser evidence |
| PR07 | Saved-state acknowledgement follows successful server response; failed save retains all drafts and reports failure; editing resets acknowledgement | Frontend save/failure tests |
| PR08 | Flyway V6 adds fields with empty defaults without changing old note body/owner/version/time/visibility; old binaries can still insert notes | Legacy insert regression, pre/post-upgrade/restart comparison and isolated backup/restore |

API: PUT `/api/notes/{kind}/{key}` retains the existing route. Additional optional
inputs are `projectName:string|null` and `projectInfo:string|null`. Responses add
both fields as strings. Omission/null preserve current values, including for
save-and-hide. This does not add cross-chain scope: the current address feature
supports Ethereum only. A future chain expansion must extend canonical identity
before sharing records across deployments.

Storage: MySQL notes.project_name VARCHAR(120), notes.project_info VARCHAR(4000),
both utf8mb4 NOT NULL DEFAULT ''. Existing body TEXT remains the custom note.
Field limits intentionally match Java String/HTML maxlength UTF-16 semantics;
astral characters use two units. OpenAPI maxLength is an additional coarse bound
and describes the stricter UTF-16 server constraint.

No source is enabled and no claim of full project, label or live-data coverage is
made by this increment. All original production gates remain in force.

## Verified increment

PR01–PR07: four MySQL integration cases, six OpenAPI cases (including the
additional real project-field response test), fifteen frontend cases and twelve
desktop/mobile browser cases pass. The whole Java suite passed57 cases before
the final additional contract case passed in its targeted6-case run; one opt-in
live probe is skipped in ordinary tests. Test-first failures and remaining scope
are recorded in `traceability.md`.

PR08 evidence:
- `evidence/project-records-migration-20261021164825067.json`: nonempty isolated
  V1–V5 to V6 SQL migration; original-column hashes equal; empty defaults and
  Unicode project write checked.
- `evidence/project-records-preview-upgrade-20261021165244686.json`: actual local
  startup upgrade; personal workspace was empty, so not a nonempty migration proof.
- `evidence/mysql-restore-verification-20261021165431466.json`: consistent dump
  restored to separate MySQL database with equal schema/data hashes and explicit
  Unicode three-field/version/visibility checks.
- `evidence/local-restart-verification-20261021165432696.json`: nonempty test
  workspace with two project records unchanged across Java process restart.

These are local migration/recovery checks, not a public deployment or concurrent
multi-version rollout test. Screenshots reviewed on desktop/mobile are preserved
under `evidence/project-records-ui/`.
