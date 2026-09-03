# Tasks: Manage Monthly Budget Limits

## Review Workload Forecast

Planned: 973 lines; PR3A 303, PR3B 240–330, PR3C 300–380; each under 400; no size exception. Auto-chain, stacked-to-main.

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit/PR | Goal | Focused test (bash gradlew) | Runtime | Rollback |
|---|---|---|---|---|
| PR3A | Data mutations; complete | `testDebugUnitTest --tests '*BudgetViewModelTest'`; `compileDebugAndroidTestKotlin` | N/A: no exact ADB device | `FinanceRepositories.kt`, `FinanceDao.kt`, `RoomFinanceRepositories.kt`, tests |
| PR3B | Rollover + cross-screen state | `testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest'` | `connectedDebugAndroidTest` only with exact `device`; otherwise skip | `CurrentMonthSource.kt`, ViewModels, wiring, tests |
| PR3C | Compose management + audit | `testDebugUnitTest`; `compileDebugAndroidTestKotlin` | `connectedDebugAndroidTest`; exact device only | Budget UI/navigation/resources/tests |

## Phase 1: Presentation Foundation (PR1)

- [x] 1.1 **RED** — Add `app/src/test/java/com/saldoclaro/finance/core/presentation/UiPresentationTest.kt` and seed assertions in `app/src/androidTest/java/com/saldoclaro/finance/data/local/FinanceDatabaseTest.kt` for fixed `es-ES` formatting, current/legacy IDs, custom names, and `UiErrorKey`.
- [x] 1.2 **GREEN** — Create `app/src/main/java/com/saldoclaro/finance/core/presentation/UiPresentation.kt`; expand `app/src/main/res/values/strings.xml`; update `FinanceComponents.kt` and `FinanceDatabase.kt` for Spanish resources, formatters, labels, and unchanged IDs.
- [x] 1.3 **REFACTOR** — Centralize resource templates; prove the default-only catalog has no selector or active English fallback.

## Phase 2: Translated UI and Safe Errors (PR2)

- [x] 2.1 **RED** — Extend `AppSemanticsTest.kt`, `DashboardScreenTest.kt`, and `TransactionViewModelTest.kt` for Spanish exact visible/accessibility text, transient/terminal/dialog states, unsupported locales, and raw-error disclosure prevention.
- [x] 2.2 **GREEN** — Wire `stringResource`/`UiErrorKey` through `SaldoClaroNavHost.kt`, `RetryableErrorState.kt`, `BudgetScreen.kt`, `DashboardScreen.kt`, `TransactionScreen.kt`, `CategoryScreen.kt`, their ViewModels, and `RoomFinanceRepositories.kt`; preserve routes, IDs, logs, and user data.
- [x] 2.3 **REFACTOR** — Audit 123 copy sites and exact selectors without broad tags; keep app-authored messages Spanish and semantic.

## Phase 3A: Data Mutations (PR3A)

- [x] 3A.1 **RED** — Add `FinanceDatabaseTest.kt` RED cases for exact-key edit/delete isolation, invalid/missing/stale/archived outcomes, and transaction preservation.
- [x] 3A.2 **GREEN** — Implement `BudgetTarget`, `BudgetMutationError`, atomic Room `editAmount`/`delete`, exact DAO predicates, and affected-row guards in `FinanceRepositories.kt`, `FinanceDao.kt`, and `RoomFinanceRepositories.kt`.
- [x] 3A.3 **REFACTOR/VERIFICATION** — Verify compile, 23/23 JVM tests, exact-key/typed outcomes; UI/rollover out of scope.

## Phase 3B: Rollover and ViewModel Projection (PR3B)

- [ ] 3B.1 **RED** — Add failing `BudgetViewModelTest.kt`/`DashboardViewModelTest.kt` for automatic current-month rollover; active categories, archived edit/delete constraints, stale targets, and no-limit visibility.
- [ ] 3B.2 **GREEN** — Add `CurrentMonthSource.kt`; wire lifecycle/foreground switching via `AppContainer.kt`, `SaldoClaroNavHost.kt`, and both ViewModels; project spending unions in Budgets/Dashboard state.
- [ ] 3B.3 **REFACTOR/VERIFICATION** — Verify boundary recheck, archived rules, recoverable errors, and `NO_BUDGET` with JVM/compile tests.

## Phase 3C: Compose Management UI (PR3C)

- [ ] 3C.1 **RED** — Add failing `BudgetScreenTest.kt`/`DashboardScreenTest.kt` cases for card tap, amount-only edit, exact category/month delete confirmation, archived delete-only mode, no-limit visibility, and Spanish semantics.
- [ ] 3C.2 **GREEN** — Wire `BudgetScreen.kt`, `SaldoClaroNavHost.kt`, `BudgetViewModel.kt`, and `strings.xml` for card/dialog management, cancellation/recovery, and Spanish visible/accessibility copy.
- [ ] 3C.3 **REFACTOR/VERIFICATION** — Run final regression/resource audit for exact selectors, no English fallback/raw errors, and JVM/Android-test compilation.

### Legacy Mapping

- `3.1` → PR3A (`3A.1–3A.3`, complete only for data mutations).
- `3.2` → PR3B (`3B.1–3B.3`, pending rollover/projection).
- `3.3` → PR3C (`3C.1–3C.3`, pending UI/audit).

Next: PR3B / 3B.1; `3.2` obsolete.

Threat matrix: N/A; no threat-specific RED tasks apply.
