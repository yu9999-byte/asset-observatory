# Page review decisions

Date: 2026-10-22. This ledger separates user-confirmed decisions from suggestions.
Scope: presentation and page interactions; source integration belongs to the
other conversation. Older UI contracts should be read alongside these updates.

## Homepage heading: confirmed and applied

Direct user decisions in the current conversation:

| Element | Decision | Result |
| --- | --- | --- |
| Market overview heading | Keep | Heading remains |
| Spot / derivatives / on-chain flow subtitle | Remove | Removed |
| Manual refresh button | Remove; browser reload is sufficient | Removed from homepage heading |
| Directory / spot / derivatives record counts | Remove | Removed |
| Single-source / not whole-market explanation | Remove; future scope is all-market data | Removed; complete data coverage is still a separate implementation task |
| Source and gaps link in heading area | Remove | Removed from this area |
| Homepage watched-address rail | Move to other pages | Removed from homepage; existing watch workspace and holder-page rail remain |

Implementation: `frontend/src/main.tsx`, `frontend/src/style.css`.
Verification at that increment: 18 frontend tests passed; production build passed.
Preview: http://127.0.0.1:5173/ . This is not a browser visual acceptance claim.

## Homepage lower region: prior recommendations

The current screenshot includes the table footer, source rows, metric note,
four rankings, an address activity block, and the shared application footer.
The recommendations below are superseded by the confirmed decisions that follow.

| Element | Evidence / current behavior | Pending recommendation |
| --- | --- | --- |
| Directory query count and server-filter wording | Implementation copy in DataTable; current count is returned rows, not a complete-market total | Simplify wording and use an actual total only when supported |
| Page size and pagination | Shared-table capability recorded under R08/R23 | Keep |
| Kraken / Kraken Futures status rows | Source provenance requirement R09; full-width expanded placement chosen by implementation | Consolidate details; retain a compact abnormal-data indication |
| Missing-value / volume / OI note | Metric semantics are required; long always-visible sentence chosen by implementation | Move details to relevant column tooltips |
| Four rankings | Explicitly recorded in R14; currently hard-coded missing states | Keep feature scope; discuss compact temporary empty presentation |
| Key address activity block | No direct request for this separate homepage block found; lookup exists elsewhere | Remove homepage block, retaining address lookup elsewhere |
| Shared footer brand / research-only text | Implementation addition | Remove if confirmed for all pages |
| Shared footer missing-data / timezone / source link | Meaning useful; placement and repeated links chosen by implementation | Consolidate or relocate; confirm cross-page scope before editing |

## Homepage lower region: confirmed and applied

- Remove the directory-query summary from the homepage table; retain page size
  and previous/next controls. Other tables retain their existing summaries.
- Remove both homepage source-status rows and the always-visible metric note.
- Retain all four ranking sections. They remain placeholders pending implementation.
- Retain the address activity section for now. The user clarified the intended
  capability is configurable monitoring that detects address activity. Current
  lookup and missing-state UI do not implement that capability. Configuration,
  monitoring rules and any future page relocation remain to be planned.
- Remove the shared application footer, including brand, research-only text,
  timezone explanation and source link. This applies across research pages.

## Asset holder page: confirmed and applied

Direct user decisions:

| Element | Decision | Result |
| --- | --- | --- |
| Back link, `BTC · 持仓地址`, watch and note icons | Keep | Remains in the page heading |
| Duplicate Bitcoin subtitle and repeated asset identity | Merge | Asset icon/name appear once; contract is shown separately |
| Price, market cap / FDV, volume and OI summary | Keep | Summary strip remains |
| Collapsible watched-address rail | Remove | Watched addresses are represented by the table's `已关注` tab |
| `全部 / 已关注` | Keep | Remains as the holder scope filter |
| Refresh button | Remove | Browser refresh is sufficient |
| `已加载地址 / Top30 / 多签 / 自定义` | Remove | Classification controls are removed |
| Separate address lookup and table search | Merge | The table search is the single address search |
| Export | Remove | Export control is hidden for this table |
| Holder table, column settings and pagination | Keep | Table configuration and pagination remain available |

Implementation: `frontend/src/main.tsx`, `frontend/src/DataTable.tsx`, and
`frontend/src/style.css`. This page review does not change source integration;
the other conversation owns that work.
