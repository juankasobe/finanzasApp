# Design: Manage Monthly Budget Limits

## Technical Approach

Keep the Room composite identity and reactive projections. Add a card-tap management dialog driven entirely by `BudgetViewModel`, atomic same-key edit/delete operations, and a shared clock/zone month source for Budgets and Dashboard. In parallel, move app-authored presentation to one default Spanish resource catalog and typed presentation mappings.

## Architecture Decisions

| Decision | Alternatives / tradeoff | Choice and rationale |
|---|---|---|
| Management state | Local Compose state is smaller but cannot preserve target/error truth | `BudgetMutationState` owns selected snapshot, confirmation, running, validation, and recoverable error. Cards without limits open no actions; archived targets expose delete only. |
| Edit/delete consistency | UI-only checks race with Room updates; new IDs/migration add needless scope | In a Room transaction, load the exact key and compare its opened amount. Edit validates active category then calls existing same-key `upsert`; delete skips active validation and executes exact-key DAO delete. Affected-row evidence must be exactly one before success. |
| Month rollover | Construction-time month is stale | A shared lifecycle-aware `CurrentMonthSource`, using injected `Clock`/`ZoneId`, emits at the local boundary and rechecks on foreground. Both ViewModels switch observations; every mutation also compares its target with `currentMonth(clock, zone)` before Room access. |
| Spanish presentation | In-place literals/raw messages can leak English; locale variants violate policy | Default `values/strings.xml` is the only catalog. Compose resolves resources and typed `UiErrorKey`; built-in IDs map to Spanish resources, custom names remain byte-for-byte unchanged, and fixed `es-ES` formatters ignore device locale. |

## Sequence Diagrams

```text
Edit/Delete: Card -> VM: open(snapshot)
VM -> UI: Editing | ConfirmDelete
UI -> VM: submit
VM -> MonthSource: validate current target
VM -> Repository -> Room transaction: find exact key; compare amount
Room -> DAO: upsert same key | delete(categoryId, monthKey): affectedRows
DAO -> Flow -> Budgets + Dashboard: spending union projects NO_BUDGET
```

```text
Rollover: Lifecycle -> MonthSource: foreground/recheck
MonthSource -> Clock(zone): next local boundary
Clock -> MonthSource: new YearMonth
MonthSource -> BudgetVM + DashboardVM: switch latest month flows
BudgetVM -> open dialog: close/refresh stale target
late mutation -> VM: typed stale rejection (no repository mutation)
```

```text
Presentation: ViewModel -> Compose: domain values + UiErrorKey
Compose -> strings.xml: Spanish text/templates/descriptions
Compose -> CategoryPresenter: stable built-in ID | exact user name
Compose -> es-ES formatter: date/cents
Compose -> Semantics/UI: deterministic Spanish output
```

## File Changes

| Action | Files |
|---|---|
| Create | `app/src/main/java/com/saldoclaro/finance/core/time/CurrentMonthSource.kt`; `app/src/main/java/com/saldoclaro/finance/core/presentation/UiPresentation.kt`; `app/src/androidTest/java/com/saldoclaro/finance/BudgetScreenTest.kt` |
| Modify: budget/data | `app/src/main/java/com/saldoclaro/finance/domain/repository/FinanceRepositories.kt`; `app/src/main/java/com/saldoclaro/finance/data/local/FinanceDao.kt`; `app/src/main/java/com/saldoclaro/finance/data/repository/RoomFinanceRepositories.kt`; `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetViewModel.kt`; `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetScreen.kt`; `app/src/main/java/com/saldoclaro/finance/feature/dashboard/DashboardViewModel.kt`; `app/src/main/java/com/saldoclaro/finance/di/AppContainer.kt`; `app/src/main/java/com/saldoclaro/finance/navigation/SaldoClaroNavHost.kt` |
| Modify: Spanish | `app/src/main/res/values/strings.xml`; `app/src/main/java/com/saldoclaro/finance/data/local/FinanceDatabase.kt`; `app/src/main/java/com/saldoclaro/finance/core/designsystem/FinanceComponents.kt`; `app/src/main/java/com/saldoclaro/finance/core/designsystem/RetryableErrorState.kt`; `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetScreen.kt`; `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetViewModel.kt`; `app/src/main/java/com/saldoclaro/finance/feature/dashboard/DashboardScreen.kt`; `app/src/main/java/com/saldoclaro/finance/feature/dashboard/DashboardViewModel.kt`; `app/src/main/java/com/saldoclaro/finance/feature/transactions/TransactionScreen.kt`; `app/src/main/java/com/saldoclaro/finance/feature/transactions/TransactionViewModel.kt`; `app/src/main/java/com/saldoclaro/finance/feature/categories/CategoryScreen.kt`; `app/src/main/java/com/saldoclaro/finance/feature/categories/CategoryViewModel.kt` |
| Modify: tests | `app/src/test/java/com/saldoclaro/finance/feature/budgets/BudgetViewModelTest.kt`; `app/src/test/java/com/saldoclaro/finance/feature/dashboard/DashboardViewModelTest.kt`; `app/src/test/java/com/saldoclaro/finance/feature/transactions/TransactionViewModelTest.kt`; `app/src/androidTest/java/com/saldoclaro/finance/AppSemanticsTest.kt`; `app/src/androidTest/java/com/saldoclaro/finance/DashboardScreenTest.kt`; `app/src/androidTest/java/com/saldoclaro/finance/data/local/FinanceDatabaseTest.kt` |

## Interfaces / Contracts

```kotlin
data class BudgetTarget(val categoryId: String, val month: YearMonth, val openedLimitCents: Long)
data class DeleteEvidence(val affectedRows: Int)
sealed interface BudgetMutationState { data object Idle; data class Editing(val target: BudgetTarget); data class ConfirmDelete(val target: BudgetTarget); data class Running(val target: BudgetTarget); data class Error(val target: BudgetTarget, val reason: UiErrorKey) }
suspend fun BudgetRepository.editAmount(target: BudgetTarget, newLimitCents: Long): Result<Unit>
suspend fun BudgetRepository.delete(target: BudgetTarget): Result<DeleteEvidence>
interface CurrentMonthSource { val month: StateFlow<YearMonth>; fun setForeground(active: Boolean); fun refresh() }
```

Dashboard MUST select `Progress(projectBudgetProgress(...))` whenever the transaction/budget union is non-empty, not merely when budgets exist. Category metadata passed from navigation lets every screen map built-ins by ID without exposing keys.

## Testing Strategy

| Layer | Proof |
|---|---|
| Unit (RED first) | Explicit states; invalid/archived edits; same-key/stale/rollover rejection; Dashboard no-limit projection; typed errors; fixed locale and stable-ID/custom-name mapping. |
| Room instrumentation | Upsert replacement; exact delete count; zero/stale result; month/category isolation; transactions preserved; archived delete. |
| Compose/E2E | Card-tap dialog, confirmation/cancel/recovery, cross-screen refresh, Spanish visible/accessibility exact text under non-Spanish device locale. Update selectors to Spanish without replacing behavior-specific selectors with broad tags. Audit that no locale resource directory, selector, fallback, hardcoded app copy, or raw exception reaches UI. |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable classification, or process-integration boundary is introduced.

## Migration / Rollout and Rollback

No schema/data migration. New databases seed Spanish built-in names; existing rows render Spanish by stable ID. Auto-chain seams: (1) presentation types/catalog/formatters, (2) translated screens plus exact-text contracts, (3) month source and budget persistence/ViewModel/UI tests. Each slice keeps its tests and can be reverted independently; rollback restores prior presentation or management wiring without changing stored keys/data.

## Risks

Boundary scheduling may be delayed by suspension (foreground refresh is the backstop); exhaustive copy inventory can drift (resource/static audit); combined scope exceeds 400 lines (mandatory review slices).

## Open Questions

None — product decisions and both active specifications resolve the implementation choices.
