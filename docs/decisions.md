# Architecture decisions

ADR-001: Java 21 / Spring Boot with Maven; React + TypeScript + Vite for a same-origin web UI.
One service serves the compiled frontend. Provider calls only occur on the backend.

ADR-002 (accepted; supersedes earlier H2 choice): Use MySQL 8.4 LTS for business records and approved history.
Versioned Flyway SQL migrations, utf8mb4, UTC timestamps and least-privilege application credentials.
Tests and deployment recovery must use real MySQL. No embedded-H2 substitute counts as acceptance.
Single application node initially; backup uses consistent mysqldump and verified restore to a separate
database. Do not alter or overwrite any existing user database.
Clarification on 2026-10-22: this is not a requirement to route every source
response through MySQL. Acquisition and entitlement come first; see ADR-006.

ADR-003: A provider failure is data, not HTTP success with fabricated measurements. Public research
GETs return envelopes with `unavailable`, `unsupported`, `stale`, `partial` or `ok`. Input errors and
storage failures use meaningful HTTP errors. Adapters preserve raw numeric strings with BigDecimal.

ADR-004: User explicitly authorized Goal launch. The existing matching active Goal is reused. Public
publishing, messaging, funds operations and automatic trading remain excluded.

ADR-005 (2026-10-21 realignment): White/light-gray surfaces, restrained blue interaction accent, red/green textual signed changes,
system sans-serif, compact tabular figures; no decorative dashboard images or marketing hero.
Semantic HTML tables and Lucide icons. Design skill's dark/gold recommendations conflict with the
requested light research UI and are not adopted.
Use compact top navigation, 46px desktop table rows and unframed sections with
fine rules. Missing proportions have no quantitative bar. See UI01-UI12 in
`docs/ui-realignment.md`; data-dependent rankings remain visibly unavailable.

ADR-006 (2026-10-22 acquisition-first): Public data is obtained by a backend
provider adapter, not by MySQL. Select the path after reproducible Java probes
and metric/coverage/rights verification. Current quotes may use a bounded shared
memory cache; owner-scoped watches, notes and preferences use MySQL; only the
authorized history needed for metric windows, deduplication and recovery is
persisted with policy evidence and retention. Redis is not needed for the current
single-node validation scope. No new runtime adapter is enabled by the probe.

CoinLore is technically reachable but lacks a source snapshot timestamp, JCT
supply and a proven provider-contract link; external display/storage entitlement
is unresolved. Routescan proves a bounded historical transfer/log mapping only;
same-provider agreement is not independent reconciliation. Holder and derivative
alternatives failed from the current Java deployment network. These results do
not justify a paid plan, a mass backfill or a production release. The original
feature backlog remains required; proceed with source-independent work and keep
source-dependent acceptance explicitly open.
