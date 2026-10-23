# Research interface realignment, 2026-10-21

This increment implements the user's supplied three-page design requirements. It
does not narrow R01-R24 or establish live-source/production acceptance. MySQL
remains the system of record. CoinAnk informs compact white tables, fine dividers,
semantic signed changes and proportion bars; Arkham informs address/flow grouping.

## Verifiable page contract

| ID | Behavior | Verification |
| --- | --- | --- |
| UI01 | Compact header, 12-14px tables, 48px or shorter desktop rows; no large hero | desktop screenshot and DOM dimensions |
| UI02 | Market columns include price, circulating cap, FDV, volume, OI, funding and 24H change; four rankings below | component test and browser screenshot |
| UI03 | Asset summary uses actual selected asset market/derivative response when present | nonempty synthetic component fixture |
| UI04 | Holder columns include chain, quantity, proportion, USD value and 1H/24H/3D/7D/1M/1Y quantity changes | component test |
| UI05 | Loaded rows and watched rows are distinct filters; custom watched addresses survive absence from the current source page | component test with out-of-page watch |
| UI06 | Unverified global Top30 and multisig identity filters are disabled with reasons; no unlabeled local slice presented as Top30 | component and browser test |
| UI07 | Address table includes From/To, timestamp, quantity, event price/value; direction filter works; exchange filter disabled until labels exist | nonempty component fixture |
| UI08 | 24H/3D/7D statistic window selection updates labels; missing history is never aggregated from one incomplete cursor page | component test |
| UI09 | Notes preview saved text, pin prevents dragging, fold preserves draft, close saves hidden state | component and MySQL browser tests |
| UI10 | Table identity column stays pinned when it is the first visible column; existing preference configuration upgrades without losing valid order/hidden/version | table unit, MySQL integration and browser tests |
| UI11 | Collapsible watch rail and all/watched filter; notes remain entity-bound; navigation preserves selected asset | component and browser tests |
| UI12 | 390px viewport has no document horizontal overflow; wide tables scroll in their own region; no page errors | Playwright mobile + screenshots |

## Data truth constraints

Existing APIs currently return holder quantity and contract-code presence, not
supply, historical balances, verified entity labels or event prices. These added
presentation columns remain missing with a reason. Missing proportion has no
zero-width value bar. USD valuation changes are never conflated with quantity
changes. Known market prices/volume/OI must not be hidden by static placeholders.
Venue OI retains its reported unit; funding and liquidation aggregates stay
missing. Coverage is the fixed catalog and source pages, never all markets.

Current backend query protocol still operates on source cursor pages. Search and
sorting in the current shared component operate on loaded rows and say so. Global
sorting, complete Top30, identity filters, statistical history, project-name/info
fields, rank calculations and continuous monitoring remain release gaps.

## Compatibility

New column IDs are appended to a stored preference's valid order on GET; valid
hidden keys, sort, page size and optimistic version remain. Removed keys are
dropped, invalid sort cleared. Normalization does not perform a database write.
The next ordinary PUT persists the normalized complete order and compares the
unchanged version. Unknown keys in new PUT requests are still rejected.

## TDD evidence

Initial layout edits preceded this increment's tests and are not claimed as a
complete historical red/green cycle. New regression cases are run failing before
their corresponding filter, summary, note and preference fixes. Verification
results and evidence paths are recorded in docs/traceability.md.
