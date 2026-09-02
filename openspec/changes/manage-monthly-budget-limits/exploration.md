## Exploration: manage-monthly-budget-limits

### Current State

Saldo Claro has a current-month budget vertical slice in the single `:app` Android module. The existing flow is:

```text
SaldoClaroNavHost
  -> active categories (archived categories filtered out)
  -> BudgetScreen
  -> BudgetViewModel.saveLimit(categoryId, rawLimit)
  -> BudgetRepository.save(categoryId, month, limitCents)
  -> RoomBudgetRepository
  -> BudgetDao.upsert(BudgetEntity)
  -> observeMonth flow
  -> projectBudgetProgress
  -> BudgetScreen and DashboardViewModel
```

- **Compose UI:** `BudgetScreen` owns the selected `CategoryEntity?`, raw limit string, and dropdown state locally. The editor only adds/saves a limit; each `BudgetProgressCard` is display-only, with no edit or delete affordance. Pressing `Save limit` with no category selected is a silent no-op. The screen renders `Content`, `Validation`, or `Error`, but has no loading, mutation-in-progress, confirmation, or success state.
- **ViewModel:** `BudgetViewModel` captures `currentMonth(clock, zone)` once at construction. It combines transaction and budget flows for that `YearMonth`, calculates progress, and exposes `BudgetUiState`. `saveLimit` accepts positive values matching `[0-9]+(\.[0-9]{1,2})?`, stores a pending save for retry, and clears that pending command on success. There is no event hierarchy or delete command. `BudgetUiState.Error` contains a progress list, but `showError` always supplies an empty list, so read and save failures clear visible progress.
- **Domain:** `Budget` is identified by `categoryId`, `YearMonth`, and `limitCents`. `projectBudgetProgress` unions expense categories and budget categories, uses expense cents only, and keys limits by category. `BudgetProgressItem` already contains the category ID, nullable limit, spent amount, and remaining amount needed to target an existing budget.
- **Repository/data:** `BudgetRepository` exposes only `observeMonth` and `save`. `RoomBudgetRepository.observeMonth` converts `YearMonth` to the ISO `YYYY-MM` string and maps entities back to domain models. `save` validates a positive limit and active category, then calls `BudgetDao.upsert`.
- **Room identity:** `BudgetEntity` has the composite primary key `(categoryId, monthKey)`, an index on `categoryId`, and a `RESTRICT` foreign key to `categories`. `BudgetDao.upsert` uses `OnConflictStrategy.REPLACE`; therefore saving a new amount for the same category and month already has update semantics. No schema change is needed for an exact-key delete. `FinanceDatabase` is version 1 and exports the unchanged budget schema.
- **Dependency wiring and propagation:** `AppContainer` creates one database, one `RoomBudgetRepository`, and supplies that repository to both the lazy `BudgetViewModel` and `DashboardViewModel`. A successful Room mutation will therefore refresh both screens through their existing flows. The shared ViewModels can outlive a navigation visit, so their captured month does not automatically roll over when the process remains alive across a month boundary.
- **Category/history interaction:** `SaldoClaroNavHost` passes only non-archived categories to the budget editor. Archiving a custom category does not remove its budgets because the category row remains and the foreign key is restrictive. An archived category's existing budget can still be observed; `BudgetScreen` renders it using a synthesized category name when it is absent from the active list, but the normal editor cannot select it and repository saves reject inactive categories.
- **Existing tests:** `MoneyDateUseCasesTest` covers budget-state calculation and ordered projection. `BudgetViewModelTest` covers expense-only progress, invalid amounts, and retryable read failure, but not successful saves, failed saves, edits, deletes, missing targets, or category selection. `DashboardViewModelTest` and `DashboardScreenTest` cover budget overview projection, empty state, and read retry. `AppSemanticsTest` only verifies navigation to Budgets and the `Save limit` label. `FinanceDatabaseTest` covers category seeding/archival and transaction history, but has no budget insert, upsert, month-isolation, or delete scenarios. There is no dedicated budget Compose screen test.

**Upsert assessment:** Editing the amount can reuse the current `save` path when the target category and month remain unchanged. The current editor must first load the selected budget's existing limit; otherwise choosing a category and saving creates or replaces a row without representing an edit. If the category selector is allowed to change during edit, the current `save` call targets a different composite key and leaves the original row in place, so that behavior must not be treated as an implicit reassignment.

**Deletion assessment:** Deletion is missing from every layer: there is no repository contract method, DAO query, ViewModel mutation/state, UI action, confirmation dialog, or test. The safe persistence shape is an exact `(categoryId, monthKey)` delete returning affected-row count, analogous to `TransactionDao.delete(id)` and `RoomFinanceRepositories.delete`. Removing a budget row would not remove transactions; spending for that category would remain available for a `NO_BUDGET` projection in the Budgets screen. If it is the last budget, the Dashboard currently changes to `NoBudgets`, while the Budgets screen can still show expense-only activity.

### Affected Areas

- `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetScreen.kt` — expose per-budget edit/delete actions, prefill/edit state, required category behavior, and confirmation/error presentation.
- `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetViewModel.kt` — model edit/delete intents, exact targets, pending mutations, retry, and confirmation state without false success.
- `app/src/main/java/com/saldoclaro/finance/domain/repository/FinanceRepositories.kt` — add an exact-month/category budget deletion contract; existing save contract can support same-key edits.
- `app/src/main/java/com/saldoclaro/finance/data/repository/RoomFinanceRepositories.kt` — implement deletion and preserve active-category validation for saves; decide how an already archived category's existing budget may be deleted.
- `app/src/main/java/com/saldoclaro/finance/data/local/FinanceDao.kt` — add a `BudgetDao` delete keyed by both `categoryId` and `monthKey`; the entity/schema can remain unchanged unless product choices require a different identity or soft deletion model.
- `app/src/main/java/com/saldoclaro/finance/data/local/Entities.kt` and `FinanceDatabase.kt` — verify composite-key and foreign-key behavior; no migration is expected for a DAO-only delete.
- `app/src/main/java/com/saldoclaro/finance/navigation/SaldoClaroNavHost.kt` and `di/AppContainer.kt` — likely wiring touchpoints if the ViewModel API or screen state changes; the existing shared repository wiring should otherwise be retained.
- `app/src/main/java/com/saldoclaro/finance/feature/dashboard/DashboardViewModel.kt` and `DashboardScreen.kt` — existing flows should reflect mutations automatically, but post-delete `NoBudgets` versus expense-only presentation needs acceptance coverage.
- `app/src/test/java/com/saldoclaro/finance/feature/budgets/BudgetViewModelTest.kt` — add mutation, confirmation, retry, exact-target, and state-preservation coverage.
- `app/src/androidTest/java/com/saldoclaro/finance/data/local/FinanceDatabaseTest.kt` and a new budget Compose test — prove Room month/category isolation, upsert replacement, deletion, confirmation, and visible refresh. `TransactionViewModel.kt`/`TransactionScreen.kt` provide the existing confirmation-dialog and retry-mutation precedent.

### Approaches

1. **Card actions with a shared inline editor** — Add Edit and Delete actions to each budget card. Edit loads that card's current amount and target identity into the existing editor; save reuses same-key upsert, while delete confirms and removes the exact current-month row.
   - Pros: Minimal data-model change; clear target; reuses existing flow propagation and the transaction deletion pattern; easy to keep under the review budget as one focused vertical slice.
   - Cons: Adds editor mode and mutation state; category reassignment needs an explicit rule; archived-category cards need a deliberate action policy.
   - Effort: Medium

2. **Per-budget dialog or bottom-sheet editor** — Keep the overview compact and open a focused edit/delete surface from each card.
   - Pros: Separates editing and destructive confirmation from the add form; reduces inline card clutter.
   - Cons: More Compose state and accessibility/test surface; still requires the same repository and ViewModel changes; dialogs make month/category context easier to hide.
   - Effort: Medium

3. **Introduce a surrogate budget ID or soft-delete records** — Give each row an independent identity or retain deleted rows with a status flag.
   - Pros: Could support audit history, undo, or future recurring-budget features.
   - Cons: Unnecessary for the existing composite identity; requires migration, query filtering, and new consistency rules; increases the chance of duplicate category/month budgets.
   - Effort: High

### Recommendation

Use approach 1 with the existing `(categoryId, YearMonth)` identity. Treat an amount-only edit as the current `BudgetRepository.save` upsert, add an exact composite-key delete, and model mutation/confirmation/retry states after the existing transaction flow. Keep deletion independent from transactions and ensure a failed mutation cannot look successful. Do not silently treat changing the category selector as an edit: require a product decision, and if reassignment is allowed, implement the old-row removal and new-row save as one explicit operation. Avoid a schema migration unless audit/undo requirements make a surrogate ID or soft deletion necessary.

### Risks

- A category-only delete could remove limits from other months; every mutation must carry both category and month.
- The singleton-like lazy ViewModels capture one month and can show stale-month data after a month boundary unless lifecycle refresh behavior is defined.
- Archived categories retain budget rows but are excluded from the editor, creating rows that may be visible yet not editable; deleting or retaining them needs a consistent rule.
- After deletion, Budgets and Dashboard intentionally derive different empty/no-budget presentations; this can surprise users unless specified and tested.
- There is currently no budget mutation instrumentation coverage, so Room refresh, confirmation, and persistence behavior remain unproven on device.

### Ready for Proposal

No — ask the following product questions before proposal:

1. **Action surface:** Should Edit and Delete be always-visible actions on each budget card, or should tapping a card open a separate details/editor surface?
2. **Edit identity:** Is editing limited to changing the amount for the existing category/month, or may a user move a limit to another category? If reassignment is allowed, should the old key be removed atomically?
3. **Delete behavior:** Should deletion permanently remove only the selected current-month limit while preserving all transactions and other months, and should it require confirmation or offer Undo?
4. **Post-delete presentation:** If a category still has expenses after its limit is deleted, should Budgets retain a “No monthly limit” card, hide that category, or use another presentation? Should Dashboard mirror that choice?
5. **Archived and month scope:** What should happen to an existing limit when its custom category is archived, and is management strictly current-month-only or should historical/future months be selectable? If it is current-month-only, should an open session refresh automatically at month rollover?

> **Scope update:** The five budget questions above are retained to preserve the original exploration. They are now resolved by the current proposal and product decisions. The audit below is the authoritative exploration for the explicitly approved Spanish-only scope expansion.

### Spanish-Only Scope Expansion

#### Confirmed Decision

This same SDD change MUST also translate all user-visible application UI text to Spanish-only. The application MUST NOT adapt to the device locale, expose a language selector, or retain an active English fallback. The budget edit/delete and Spanish-only work remain one product change even though the concerns are orthogonal.

**Audit result:** the current checkout has 13 production Kotlin files containing copy or user-visible presentation logic, one existing string-resource file, and two instrumentation test files coupled to exact UI text. There are 123 current UI-reachable authored copy sites (including accessibility descriptions, error fallbacks, and app-authored built-in category labels), plus three English error literals that are currently test-only or unreachable from a screen.

#### Current Copy Architecture

- Compose copy is hardcoded in feature screens, navigation, and shared design-system components. There are no `stringResource` or `getString` calls in `app/src`.
- `app/src/main/res/values/strings.xml` contains only the `app_name` resource (`finanzasApp`), which is already referenced indirectly by `AndroidManifest.xml`. There are no locale-specific `values-*` directories.
- There are no `@Preview` composables, preview fixtures, golden files, or other UI-copy snapshots in the checkout.
- `FinanceComponents.formatCents` uses `Locale.US`; it emits US-style currency formatting such as `$0.00`. `formatDate` uses the English `MMM d` pattern and can emit values such as `Mar 15`.
- `categoryPresentationName` derives display names from technical IDs by title-casing them, so an ID such as `groceries` can become the English label `Groceries`. Several screens also render `CategoryEntity.name` directly.
- ViewModels and the category screen pass `Throwable.message` through to the UI. Repository messages and arbitrary database/parse exception messages can therefore leak English even after static literals are translated.
- The two built-in category names are app-authored persisted values: `Groceries` and `Salary` are seeded twice in `FinanceDatabase.kt` (four source occurrences, two distinct labels). User-created names are user data and MUST remain exactly as entered rather than being machine-translated.

#### Copy Inventory

Counts below treat each literal or interpolated template branch as one source copy site; duplicate occurrences count separately. Dynamic user data is listed separately and is not counted as authored English copy.

| Surface | Current file(s) | English copy sites | What must change |
|---|---|---:|---|
| Navigation | `navigation/SaldoClaroNavHost.kt` | 6 | Four destination labels, the add-transaction FAB description, and the navigation accessibility template |
| Shared components | `core/designsystem/FinanceComponents.kt` | 3 | Default delete content description plus US currency and English date formatting |
| Shared error state | `core/designsystem/RetryableErrorState.kt` | 1 | `Retry` action |
| Budget screen | `feature/budgets/BudgetScreen.kt` | 35 | Editor, labels, empty/validation states, summaries, statuses, remaining text, and progress descriptions; future edit/delete actions and dialogs must join this catalog |
| Dashboard screen | `feature/dashboard/DashboardScreen.kt` | 28 | Loading, headers, metrics, empty/error-adjacent states, budget progress, statuses, remaining text, transaction types, and semantics |
| Transaction screen | `feature/transactions/TransactionScreen.kt` | 23 | Editor, date/category labels, activity/empty/validation states, delete semantics, and the existing confirmation dialog |
| Category screen | `feature/categories/CategoryScreen.kt` | 9 | Header, create form, empty state, built-in/custom/archived labels, and archive semantics |
| ViewModel fallbacks | `BudgetViewModel.kt`, `DashboardViewModel.kt`, `TransactionViewModel.kt`, `CategoryViewModel.kt` | 6 | Spanish resource-backed error keys instead of hardcoded fallback text |
| Repository errors reaching UI | `data/repository/RoomFinanceRepositories.kt` | 8 | Replace user-visible exception text/raw message propagation with typed error outcomes resolved by the UI |
| Built-in category seed labels | `data/local/FinanceDatabase.kt` | 4 occurrences / 2 distinct | New databases need Spanish labels; existing rows need a stable-ID presentation mapping or a data migration |
| **Total** | **13 production Kotlin files** | **123** | **Spanish-only production UI/presentation scope** |

There are 12 non-null accessibility-description sites/templates in that inventory: navigation FAB and destinations; the shared delete default; budget selector and progress descriptions; dashboard balance, metric, and progress descriptions; transaction selector and row-delete descriptions; and category archive descriptions. Decorative icons intentionally use `contentDescription = null` and do not need translated copy.

The following are deliberately excluded from the 123-site product count:

- Two `RoomFinanceRepositories.saveTransaction` validation messages and one `FinanceUseCases.requirePositiveCents` message are currently exercised only by tests or have no screen caller. They should remain technical until a typed error boundary is introduced, then must not be allowed to leak to users.
- Routes (`dashboard`, `transactions`, `categories`, `budgets`), database/table/column names, `builtin-groceries`/`builtin-salary`, normalized IDs, `INCOME`/`EXPENSE`, ISO month keys, and other persistence/serialization values are technical identifiers and remain unchanged.
- `app_name` (`finanzasApp`) is the product/launcher brand, not an English sentence. `AndroidManifest.xml` already obtains it from a resource and needs no edit unless branding is separately changed.
- No application logging copy was found. Unit-test names, assertion diagnostics, fake-repository guard messages, and SDD artifacts remain English.

#### Exact-Text Test Coupling

| Test file | Current coupling |
|---|---|
| `app/src/androidTest/java/com/saldoclaro/finance/AppSemanticsTest.kt` | 19 selector calls: 12 assertions against actual UI copy (11 distinct values, with the balance description repeated) plus seven intentional negative checks for deferred features that are not rendered |
| `app/src/androidTest/java/com/saldoclaro/finance/DashboardScreenTest.kt` | 20 exact text/content-description selector calls covering loading, metrics, budget states, formatted amounts, raw error text, retry, and refreshed values |
| Existing unit tests | No exact UI selector calls. `BudgetViewModelTest`, `DashboardViewModelTest`, `TransactionViewModelTest`, and `MoneyDateUseCasesTest` use English test names, diagnostics, technical IDs, and error fixtures rather than UI copy |
| `FinanceDatabaseTest.kt` | No UI selector calls. `Coffee` and `Travel` are user-data fixtures and must not be translated; stable built-in IDs are tested, while seeded display names are not currently asserted |

Across the two instrumentation files there are 39 selector calls in total, 32 against current UI copy, and 23 distinct current UI expectation values after duplicate values are collapsed. `storage unavailable` appears in four test files as an error fixture and is asserted as visible text once; that assertion will need to follow the Spanish error policy rather than preserve a raw exception message.

#### Affected Areas for the Expanded Scope

- `app/src/main/java/com/saldoclaro/finance/navigation/SaldoClaroNavHost.kt` — translate navigation labels, FAB accessibility, and destination accessibility without changing route identifiers.
- `app/src/main/java/com/saldoclaro/finance/core/designsystem/FinanceComponents.kt` and `RetryableErrorState.kt` — centralize shared action/description copy and make currency/date output deterministic Spanish.
- `app/src/main/java/com/saldoclaro/finance/feature/budgets/BudgetScreen.kt` and `BudgetViewModel.kt` — translate the existing budget surface and ensure the new edit/delete surface, confirmation, mutation, and stale-target states never introduce English.
- `app/src/main/java/com/saldoclaro/finance/feature/dashboard/DashboardScreen.kt` and `DashboardViewModel.kt` — translate loading, empty, progress, semantics, and fallback error presentation.
- `app/src/main/java/com/saldoclaro/finance/feature/transactions/TransactionScreen.kt` and `TransactionViewModel.kt` — translate the editor, dynamic transaction labels, validation, retry, and existing delete dialog.
- `app/src/main/java/com/saldoclaro/finance/feature/categories/CategoryScreen.kt` and `CategoryViewModel.kt` — translate category management and sanitize error outcomes.
- `app/src/main/java/com/saldoclaro/finance/data/repository/RoomFinanceRepositories.kt` — stop exposing raw English validation/database messages to screens; preserve technical persistence values.
- `app/src/main/java/com/saldoclaro/finance/data/local/FinanceDatabase.kt` — seed Spanish built-in labels while preserving stable IDs, and define how existing English rows are presented.
- `app/src/main/res/values/strings.xml` — expand the single default catalog with Spanish strings only; do not add English or locale-specific alternatives.
- `app/src/androidTest/java/com/saldoclaro/finance/AppSemanticsTest.kt` and `DashboardScreenTest.kt` — update exact Spanish text/semantics and add coverage for Spanish date/category/error presentation. The current budget change also needs a new budget Compose test; no such test exists today.

The manifest, debug test activity, domain models, DAO identity definitions, and technical test fixtures do not need copy changes. Unit tests may need API updates if error states change from raw `String` values to resource/message keys, but they are not current exact-text translation targets.

#### Approaches for Spanish-Only Copy

1. **Translate literals in place** — Replace hardcoded English with Spanish, change the date/currency helpers to a fixed Spanish locale, and add Spanish fallbacks directly in Kotlin.
   - Pros: Smallest immediate diff; minimal new abstraction; straightforward for the existing single-screen codebase.
   - Cons: Leaves copy fragmented and hardcoded; raw `Throwable.message` can still leak English; built-in legacy rows and accessibility templates are easy to miss; future budget dialogs can drift.
   - Effort: Low/Medium

2. **One Spanish resource catalog with typed presentation messages** — Move static Compose copy to the existing default `strings.xml`, resolve it with `stringResource`, represent ViewModel/repository failures as stable message keys or typed outcomes, use fixed `es-ES` formatting helpers, and map built-in category IDs to Spanish resources.
   - Pros: One auditable Spanish source; no device-locale adaptation, selector, or active English fallback; protects accessibility copy and future budget actions; prevents arbitrary exception text from becoming UI.
   - Cons: Requires resource plumbing and test expectation updates; resource IDs cannot be resolved directly in domain code; the catalog and error boundary add review lines.
   - Effort: Medium

3. **Full locale-aware localization** — Add language variants, device-locale selection, or an English fallback.
   - Pros: Conventional for a multilingual product.
   - Cons: Explicitly contradicts the confirmed product decision; creates behavior and test surface that this change does not need.
   - Effort: High — rejected

#### Review-Line Forecast

The 400-line budget counts authored additions plus deletions, not generated goldens. The following is a planning estimate, not a measured implementation diff:

- Replacing 123 current production copy sites is a mechanical floor of about 246 changed lines.
- Updating the 32 current-UI exact-text selector calls is about 64 changed lines. The seven negative deferred-feature checks should remain unchanged.
- Fixed currency/date formatting, built-in-category compatibility, typed error mapping, imports, and accessibility/test hardening add approximately 40–100 changed lines.
- The recommended resource catalog adds approximately 60–90 authored resource lines. The Spanish-only work therefore forecasts roughly **410–500 changed lines**; the literal-only alternative forecasts roughly **350–410** but carries the correctness risks above.
- The existing budget edit/delete implementation and its missing DAO/ViewModel/Compose/Room coverage are independently estimated at roughly 250–350 changed lines. The combined change forecasts approximately **660–850 changed lines**, so a single review would exceed the configured budget even though the product scope remains one SDD change.

**Decision needed before apply: No — the scope expansion is explicitly approved.**

**Chained PRs recommended: Yes.**

**400-line budget risk: High.**

#### Recommendation

Use approach 2, but keep it deliberately narrow: one Spanish-only default resource catalog, no locale variants or selector, fixed `Locale("es", "ES")` presentation for currency/date, and resource-backed accessibility descriptions. Keep routes, enums, IDs, month keys, schema names, logs, test names, and SDD artifacts in English. Keep user-entered category names as user data, while mapping app-authored built-in IDs to Spanish labels so existing persisted `Groceries`/`Salary` rows cannot leak English.

Introduce a typed UI-message boundary for errors rather than passing arbitrary exception messages to Compose. Apply the same catalog and error policy to the existing transaction dialog and every new budget edit/delete state. Plan the implementation as reviewable chained slices (for example, copy/format infrastructure, feature screens and exact-text tests, then budget management and its behavior tests) while retaining the single approved SDD scope.

#### Risks

- Translating static literals alone will not prevent raw Room, parsing, or repository exception messages from appearing in English.
- Updating seed SQL changes only new databases; existing version-1 databases can retain English built-in names unless presentation maps stable IDs or a data migration is specified.
- Translating category labels must not rename persisted IDs or translate user-created names. ID-derived fallback names and icon heuristics can otherwise produce inconsistent labels or visuals.
- `Locale.US` currently leaks English month abbreviations and US formatting; fixed Spanish formatting will change exact semantics and amount/date test expectations.
- The existing transaction confirmation dialog is an easy copy surface to miss, and the new budget confirmation/edit states do not exist yet.
- Exact accessibility selectors make copy changes visible test failures; broadening selectors without preserving semantic coverage would hide regressions.
- The recommended catalog plus budget implementation is above the 400-line review budget. Treat the configured `auto-chain` strategy as a delivery constraint, not as permission to make one oversized review.
- `openspec/config.yaml` still describes the convention as `English UI copy`. It is intentionally unchanged by this task, but it conflicts with the approved decision and must be reconciled before design/tasks rely on that context.

#### Updated Proposal Readiness

The original budget decisions are resolved: card taps open a separate management surface; edits are amount-only with category/month fixed; deletion is confirmed and exact-key; transactions and other months are preserved; remaining spending stays visible without a limit; rollover is automatic; and archived-category limits are deletable but not editable.

**Ready for proposal revision: Yes. Ready for design: No.** Before design, `proposal.md` MUST be revised to include the Spanish-only intent, in/out-of-scope locale constraints, affected-surface inventory, rollback implications, and high review forecast. The delta `spec.md` MUST add Spanish-only requirements and Given/When/Then scenarios for visible copy, accessibility descriptions, loading/empty/error/dialog states, fixed date/category presentation, built-in labels, user data, and the absence of locale adaptation or English fallback. This exploration task does not modify either artifact.
