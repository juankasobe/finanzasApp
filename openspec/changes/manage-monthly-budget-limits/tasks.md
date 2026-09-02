# Tasks: Manage Monthly Budget Limits

## Review Workload Forecast

Measured delivery: 973 lines (951 additions + 22 deletions); app I1 176 authored; OpenSpec trail 797; slices A~343, B~394, I1~236, I2 250–310, I3 260–340. Delivery: auto-chain, stacked-to-main.

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

`P` = `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && env -u ADB_SERVER_SOCKET -u ANDROID_ADB_SERVER_ADDRESS -u ANDROID_ADB_SERVER_PORT bash gradlew --no-daemon --rerun-tasks --no-build-cache`; `S` = `gentle-ai sdd-status --cwd /home/juanka/dev/finanzasApp --json` + `git diff --check`.
RED → GREEN → REFACTOR, code/tests/docs together; no broken intermediate merges. Connected harnesses require exact native ADB `device`, otherwise typed unavailable/no retry.

### Suggested Work Units

| Slice (base → finish; next/out) | Lines | Focused proof | Runtime harness | Rollback |
|---|---:|---|---|---|
| A `main → 📍A`: `openspec/config.yaml` + `openspec/changes/manage-monthly-budget-limits/{exploration.md,proposal.md}`; next B/out app. | ~343 | `S` on A paths. | N/A: passive artifacts. | Those three files. |
| B `A → 📍B`: `openspec/changes/manage-monthly-budget-limits/specs/*/spec.md`, `design.md`, `tasks.md`; next I1/out app. | ~394 | `S` on B paths. | N/A: passive artifacts. | Specs, design, tasks. |
| I1 `B → 📍I1`: foundation code/tests/resources + `apply-progress.md`; next I2/out translated screens. | ~236 | `P testDebugUnitTest --tests '*UiPresentationTest'` | N/A: no new screen flow. | Foundation files/tests/resources + progress. |
| I2 `I1 → 📍I2`: translated UI/accessibility/contracts/errors; next I3/out budget. | 250–310 | `P connectedDebugAndroidTest` | App/Dashboard exact-device gate; unavailable/no retry. | Translated UI/error files + selectors. |
| I3 `I2 → 📍I3`: month source, Room mutations, management/cross-screen UI; finish/out migration, reassignment, locale, undo. | 260–340 | `P testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest'` | `P connectedDebugAndroidTest` UI/Room; same gate. | Month/budget source, wiring, tests. |

## Phase 1: Presentation Foundation (PR1)

- [x] 1.1 **RED** — Add `app/src/test/java/com/saldoclaro/finance/core/presentation/UiPresentationTest.kt` and seed assertions in `app/src/androidTest/java/com/saldoclaro/finance/data/local/FinanceDatabaseTest.kt` for fixed `es-ES` formatting, current/legacy IDs, custom names, and `UiErrorKey`.
- [x] 1.2 **GREEN** — Create `app/src/main/java/com/saldoclaro/finance/core/presentation/UiPresentation.kt`; expand `app/src/main/res/values/strings.xml`; update `FinanceComponents.kt` and `FinanceDatabase.kt` for Spanish resources, formatters, labels, and unchanged IDs.
- [x] 1.3 **REFACTOR** — Centralize resource templates; prove the default-only catalog has no selector or active English fallback.

## Phase 2: Translated UI and Safe Errors (PR2)

- [ ] 2.1 **RED** — Extend `AppSemanticsTest.kt`, `DashboardScreenTest.kt`, and `TransactionViewModelTest.kt` for Spanish exact visible/accessibility text, transient/terminal/dialog states, unsupported locales, and raw-error disclosure prevention.
- [ ] 2.2 **GREEN** — Wire `stringResource`/`UiErrorKey` through `SaldoClaroNavHost.kt`, `RetryableErrorState.kt`, `BudgetScreen.kt`, `DashboardScreen.kt`, `TransactionScreen.kt`, `CategoryScreen.kt`, their ViewModels, and `RoomFinanceRepositories.kt`; preserve routes, IDs, logs, and user data.
- [ ] 2.3 **REFACTOR** — Audit 123 copy sites and exact selectors without broad tags; keep app-authored messages Spanish and semantic.

## Phase 3: Current-Month Budget Management (PR3)

- [ ] 3.1 **RED** — Extend `BudgetViewModelTest.kt`, `DashboardViewModelTest.kt`, `FinanceDatabaseTest.kt`, new `BudgetScreenTest.kt`, and `DashboardScreenTest.kt` for open/no-limit, valid/invalid edit, cancel/confirm delete, visibility, rollover/stale, archived rules, failed/missing targets, and Spanish management text.
- [ ] 3.2 **GREEN** — Create `core/time/CurrentMonthSource.kt`; update `domain/repository/FinanceRepositories.kt`, `data/local/FinanceDao.kt`, `data/repository/RoomFinanceRepositories.kt`, budget/dashboard files, `AppContainer.kt`, and `SaldoClaroNavHost.kt` for lifecycle switching, exact-key mutations, affected-row evidence, and reactive cross-screen projection.
- [ ] 3.3 **REFACTOR** — Verify boundary cancellation, recoverable-state preservation, no false success, and no-limit Dashboard progress with the conditional Room/Compose harness.

Threat matrix: N/A in the design; no threat-specific RED tasks apply.
