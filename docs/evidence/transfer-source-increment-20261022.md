# Transfer ingress and source review verification - 2026-10-22

This report records the completed local increment. It is not production
approval or full real-data workflow acceptance. Times in raw source archives
are UTC; the report date uses Asia/Shanghai.

## Contracts and TDD

- `../transfer-ingress.md`: TX01-TX07, specified before implementation.
  Seven failing tests are retained in `transfer-ingress-red.xml`; seven passing
  tests in `transfer-ingress-green.xml`. The mapper validates full hash identity,
  exact numeric bounds, uint256 quantities, event times and duplicate consistency.
- `../data-acquisition.md`: DA10, dated terms reviews and nullable terms links.
  Backend red/green reports: `source-terms-review-red.xml` and
  `source-terms-review-green.xml`. The frontend regression also failed before
  the missing-terms display was implemented and passed afterwards.
- API contracts in `../api-contract.md` and `../openapi.json` reflect both.

## Verification results

| Check | Result | Scope |
| --- | --- | --- |
| Full Maven suite | 71 total, 70 passed, 1 skipped, 0 failures/errors | Isolated MySQL where required; live probe opt-in and skipped |
| Frontend unit suite | 17/17 passed | Local software behavior |
| TypeScript/Vite production build | Passed | Production asset compilation |
| Maven package | Passed | Rebuilt executable JAR with current frontend |
| Targeted Playwright source review | 2/2 passed | Desktop 1440x1000 and iPhone 13; real Java service and isolated MySQL |
| Personal preview health | UP after restart | Loopback port 8080 |
| Actual `/api/sources` response | Updated metadata verified | CoinLore restriction/time retained; disabled MEXC with null terms |

The Java suite inventory is in `transfer-source-verification-20261022.json`.
The browser run was targeted; this increment does not claim a fresh complete
browser-suite rerun. Passing software tests do not establish source availability,
display rights, completeness or production readiness.

The source-review screenshots were visually inspected and preserved from
Playwright's replaceable test-results directory:

- `source-review-ui/sources-desktop.png`
- `source-review-ui/sources-mobile.png`

The cases also assert no page errors and no horizontal document overflow.

Packaging initially encountered the Windows lock on the running JAR. Exact
project Java executable/JAR identities were checked before stopping the two
preview processes. Packaging then passed and both previews restarted. MySQL
remained running. This is application packaging/restart evidence, not database
crash recovery or a production deployment drill.

## Source findings and remaining gates

Official documents, raw SHA256 archives, exact acquisition times and failed
corrected-endpoint probes are linked in
`source-documents-review-2026-10-22.md`. CoinLore consent/API-specific permitted
use remains unresolved. MEXC has no retrieved JCT instrument sample and no
verified external-use entitlement. Document review times never replace quote
timestamps. No runtime source was enabled and no historical sample feeds the UI.

Current complete fixed-block Top30, historical balances/event prices, JCT OI and
liquidations, persisted reorg-safe ingestion, complete holder/transfer queries,
managed accounts and production drills remain open. Docker is unavailable.
Unlocks require eligible source terms and validated coverage/identity, plus an
actual deployment environment. Existing local tests and historical restore
evidence cannot substitute for these gates.
