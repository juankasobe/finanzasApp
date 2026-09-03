# Apply Progress: Manage Monthly Budget Limits

## Status

- Work unit/objective: `pr3b-budget-rollover-projection`, ordinal 8, generation 8, max 400 changed lines
- Delivery: `auto-chain`, `stacked-to-main`
- Attempt 8: passed/complete; `decision_required: false`, `next_action: complete`; historical native candidate `392/400`; current artifact-reconciled full candidate: tracked `306 additions + 49 deletions = 355` + untracked `43` (`CurrentMonthSource.kt`) = `398/400` relative to `origin/main`; connected instrumentation `15/15` on device `c0e19fe4`; finish `sha256:3231ee4eab393dd643e29b128be85e3379c09e03722cd62cf9fb9703d0dc5c6f`; evidence `sha256:93a0bd584d5fbabca29bad22a4c272a10cb3aba1a79aa6285b556cbfe34a7f90`.
- Completed: 12/15 tasks (`1.1`–`2.3`, `3A.1`–`3B.3`); remaining `3C.1`–`3C.3`; next: PR3C / 3C.1.
- Historical PR1 full-candidate count: 230 changed lines; executor-authored implementation diff: 176 additions + deletions, excluding SDD bookkeeping
- Historical PR3A full diff: 371 changed lines, below the 400-line limit, including the authorized task replan and bookkeeping reconciliation; native charged delta: 295 changed lines.

## Native Runtime Authority

- The maintainer-approved reset expanded the full-candidate PR1 budget from 200 to 250 lines.
- Final native objective: generation 2, work unit `pr1-presentation-foundation`, max changed lines 250, lifetime count 230.
- Final native revision and evidence revision are recorded above; the objective is complete with no decision required and `next_action: complete`.
- RDD review start was rejected with typed `rdd_disabled`; no review transaction started, receipt status remains disabled/unmanaged, and RDD was not enabled.

## Completed Tasks

- [x] 1.1 RED — Added `UiPresentationTest.kt` and Spanish built-in seed assertions to `FinanceDatabaseTest.kt`.
- [x] 1.2 GREEN — Added fixed Spanish presentation types and formatters, resource entries, resource-backed shared category labels, and Spanish built-in seed values while preserving IDs and normalized keys.
- [x] 1.3 REFACTOR — Centralized resource IDs, added resource resolution for Compose, used `Locale.ROOT` for category visual matching, and verified the default-only catalog has no selector or active English fallback.

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 1.1 | `app/src/test/java/com/saldoclaro/finance/core/presentation/UiPresentationTest.kt` and `FinanceDatabaseTest.kt` | Unit + Room instrumentation | JVM baseline: 18/18; no executor-started instrumentation safety-net run; final native authority separately completed at 230 changed lines | ✅ Tests written first; corrected focused task failed compilation on missing production symbols | ✅ Corrected focused test: 4/4 | ✅ US and Japan defaults, positive and negative currency, current/legacy IDs, two custom names, and four error resources | ✅ Approval behavior retained by later refactor cycle |
| 1.2 | `UiPresentationTest.kt` | Unit | JVM baseline: 18/18 | ✅ Existing RED test referenced the new presentation API before production code | ✅ Corrected focused test: 4/4 | ✅ Non-trivial grouped/negative amounts, two default locales, and two custom-name inputs passed | ✅ Final focused test remained 4/4 after refactor |
| 1.3 | `UiPresentationTest.kt` | Unit + static resource audit | Approval suite: 4/4 before refactor | ✅ Approval tests established before behavior-preserving refactor | ✅ N/A — no behavior change; approval suite stayed green | ✅ Existing multi-input cases remained green | ✅ 4/4 after each successful refactor step; initial `const val` catalog attempt was corrected after a runtime `NoSuchMethodError` |

## Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | Requested command `... bash gradlew test --tests '*UiPresentationTest' ...` was rejected because Android's aggregate `test` task does not accept `--tests`. Adapted command `... bash gradlew testDebugUnitTest --tests '*UiPresentationTest' --no-daemon --rerun-tasks --no-build-cache` passed: 4/4 focused tests, `BUILD SUCCESSFUL`. Full adapted suite passed 22/22 JVM tests. |
| Runtime harness command/scenario and exact result | N/A — PR1 adds no screen flow; JVM/resource proof is the accepted harness. Native authority is complete under the maintainer-approved reset (generation 2, 230/250 changed lines, `decision_required: false`, `next_action: complete`). This reconciliation started, finished, and reset no runtime attempt. |
| Rollback boundary | Revert only `UiPresentation.kt`, `FinanceComponents.kt`, `FinanceDatabase.kt`, `values/strings.xml`, `UiPresentationTest.kt`, and the `FinanceDatabaseTest.kt` seed assertions; no budget-management or unrelated files are included. |

## Resource-Policy Proof

- `app/src/main/res/values*/strings.xml` matched only `app/src/main/res/values/strings.xml`.
- `app/src/main/res/values-*` matched no directory.
- The PR1 presentation foundation contained no `selector`, language selector, active fallback, `Locale.getDefault`, `Locale.US`, `values-en`, or English resource matches.
- Existing feature-screen literals and raw error handling remain intentionally out of scope for PR2.

## Files Changed

- `app/src/test/java/com/saldoclaro/finance/core/presentation/UiPresentationTest.kt`
- `app/src/androidTest/java/com/saldoclaro/finance/data/local/FinanceDatabaseTest.kt`
- `app/src/main/java/com/saldoclaro/finance/core/presentation/UiPresentation.kt`
- `app/src/main/java/com/saldoclaro/finance/core/designsystem/FinanceComponents.kt`
- `app/src/main/java/com/saldoclaro/finance/data/local/FinanceDatabase.kt`
- `app/src/main/res/values/strings.xml`

## PR2A Status

- Scope validated: shared Spanish resources/presentation, common retry UI, navigation, Dashboard UI/ViewModel, App semantics tests, and Dashboard instrumentation tests.
- Tasks `1.1`–`1.3` remain complete; tasks `2.1`–`3.3` remain unchecked because this is only PR2A and PR2B is pending.
- PR2B split is preserved in `stash@{0}` named `pr2b-budget-category-transaction-copy`; it was not applied, inspected, dropped, or modified.
- Initial inherited compile RED exposed three integration gaps: untyped legacy retry call sites and a future `categoryMetadata` navigation argument. The compatibility error boundary and navigation correction restore the PR2A build without touching PR2B files.

## PR2A TDD Cycle Evidence

| Slice | Test layer | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|
| 2.1 partial | App semantics + Dashboard instrumentation sources | Existing aborted-actor edits; no RED history fabricated | Android test sources compile; runtime is typed unavailable | Dashboard contract covers loading, no-budget, progress, and retry recovery | Exact Spanish selectors retained; no broad tags added |
| 2.2 partial | JVM compile + Compose production path | Inherited compile failure recorded above | Focused JVM tests 8/8 and full JVM suite 22/22 pass | Dashboard/ViewModel and retry recovery exercise alternate states | Legacy callers use safe Spanish fallback; typed Dashboard errors remain |
| 2.3 partial | Static presentation/resource audit | Existing changed contract, not a new RED claim | `git diff --check` passes and default-only resources compile | US/Japan locale cases and exact semantics remain covered | PR2A stays limited to shared/Dashboard files |

## PR2A Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && env -u ADB_SERVER_SOCKET -u ANDROID_ADB_SERVER_ADDRESS -u ANDROID_ADB_SERVER_PORT bash gradlew testDebugUnitTest --tests '*DashboardViewModelTest' --tests '*UiPresentationTest' --no-daemon --rerun-tasks --no-build-cache`: 8/8 passed, `BUILD SUCCESSFUL`. Full `bash gradlew test` suite: 22/22 passed. `compileDebugAndroidTestKotlin`: `BUILD SUCCESSFUL`. |
| Runtime harness command/scenario and exact result | `adb devices -l` with the required environment cleanup timed out after 120 seconds with no output; no exact `device` state was established, so connected instrumentation was not run or retried. |
| Rollback boundary | Revert only the PR2A hunks in the nine changed app/test/resource files plus this progress section; preserve the PR1 foundation hunks and leave the PR2B stash untouched. |

## Native Runtime Attempt 4

- Ordinal: `4`
- Work unit: `pr2a-dashboard-shared-copy`
- Outcome: `passed`
- Complete: `true`
- Next action: `complete`
- Native revision: `sha256:8f30a0b13a5401a4880d88212f935349588df453996aa14a0fceca3ba7515a97`
- Evidence revision: `sha256:6552da90646f683f5f269e56535f1a29e4852fe1b85539da2eb6cc8baafa013f`
- Charged delta: `45` lines
- Full PR2A diff: `351` lines (`259` additions, `92` deletions)
- Focused JVM: `8/8 passed`
- Full JVM: `22/22 passed`
- `compileDebugAndroidTestKotlin`: passed
- Instrumentation unavailable: clean `adb devices -l` timed out after 120 seconds with no exact device; not retried.
- Protected PR2B remains in `stash@{0}` named `pr2b-budget-category-transaction-copy`.

## PR2B Status

- Work unit: `pr2b-feature-screen-copy`; delivery remains `auto-chain`, `stacked-to-main`.
- Combined PR2A and PR2B implementation satisfies tasks `2.1`, `2.2`, and `2.3`; tasks `3.1`–`3.3` remain unchecked and untouched.
- PR2B changed only the six feature files and `TransactionViewModelTest.kt`; no PR2A source was modified.
- The full worktree diff was `254` changed lines relative to `origin/main` before this bookkeeping; the final measured diff is `307` changed lines (`204` additions + `103` deletions), below the native `400`-line limit.
- Native attempt 5 charged `0` additional lines because the preserved PR2B candidate was already present at launch; the full candidate remains within the `400`-line worktree guard.

## PR2B TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 2.1 | `AppSemanticsTest.kt`, `DashboardScreenTest.kt`, `TransactionViewModelTest.kt` | Compose/Android source contracts + JVM unit | Existing candidate focused run: `TransactionViewModelTest` 7/7; prior PR2A focused evidence retained above | ⚠️ Inherited PR2B edits were present at launch; no RED history fabricated | ✅ Focused 7/7; full JVM suite 23 tests passed | ✅ Navigation/empty/loading/progress/error/retry, locale, validation, confirmation, cancellation, and raw-error-safe states covered by the combined tests | ✅ Existing candidate reviewed; no behavior-changing refactor was justified |
| 2.2 | `TransactionViewModelTest.kt` plus PR2A App/Dashboard contracts | JVM + Android-test compilation | Existing candidate focused run: 7/7 | ⚠️ Inherited PR2B edits were present at launch; no RED history fabricated | ✅ `compileDebugAndroidTestKotlin` and full JVM suite passed | ✅ Success, validation, delete confirmation, empty state, read failure, and operation failure paths retain typed UI errors | ✅ All six feature screens and three ViewModels use resource-backed copy or `UiErrorKey`; routes and persisted identifiers are unchanged |
| 2.3 | Combined presentation/resource contracts and six PR2B feature sources | Static audit + JVM/resource compilation | Existing candidate focused run: 7/7 | ⚠️ Inherited PR2B edits were present at launch; no RED history fabricated | ✅ `git diff --check`; Android-test compilation and full JVM suite passed | ✅ Existing exact Spanish selectors cover multiple destinations and alternate state paths without broad tags | ✅ No direct app-authored English visible/accessibility literals or raw exception messages remain in the PR2B feature files |

## PR2B Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh; unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_PORT; bash gradlew testDebugUnitTest --tests '*TransactionViewModelTest' --no-daemon --rerun-tasks --no-build-cache`: `BUILD SUCCESSFUL`, 7/7 tests in `TransactionViewModelTest`. |
| Runtime harness command/scenario and exact result | Clean preflight `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh; unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_PORT; timeout 120s adb devices -l` returned no device listing, so no exact `device` state was established; `connectedDebugAndroidTest` was not run and was not retried. |
| Rollback boundary | Revert only the PR2B hunks in `BudgetScreen.kt`, `BudgetViewModel.kt`, `CategoryScreen.kt`, `CategoryViewModel.kt`, `TransactionScreen.kt`, `TransactionViewModel.kt`, and `TransactionViewModelTest.kt`, plus the PR2B checkbox/progress sections; preserve PR1/PR2A code and the backup stash. |

## PR2B Validation Results

- Full JVM command: `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh; unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_PORT; bash gradlew test --no-daemon --rerun-tasks --no-build-cache` — `BUILD SUCCESSFUL`, 23/23 JVM tests.
- Android-test compilation: `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh; unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_PORT; bash gradlew compileDebugAndroidTestKotlin --no-daemon --rerun-tasks --no-build-cache` — `BUILD SUCCESSFUL`.
- Instrumentation disposition: unavailable after the one permitted clean ADB preflight; no connected test retry.
- Stash safety: `stash@{0}` remains `pr2b-budget-category-transaction-copy`; no apply, pop, drop, or reset was performed.

## Native Runtime Attempt 5

- Ordinal: `5`; work unit: `pr2b-feature-screen-copy`; outcome: `passed`; complete: `true`; request ID: `2c02123e-343a-4b68-990d-572d91712313`.
- Expected launch revision: `sha256:dad520b59e81f33253773c562dada211c13385b580db6f9b88fa2b082d83f21e`.
- Finish ledger revision: `sha256:b564aa06c6db1f1d6ce69b7c1856c7e7ef38d159786bc6fb95cedc0efb236a89`.
- Finish candidate identity: `sha256:523df0ea2f96d900c5beb2846b1365f70f6e18bd1dcdf6d877a59e5e66b8f813`; candidate tree: `77c23e364ac330865c69b895fd6abd32f651cbbb`.
- Evidence revision: `sha256:575c8bf7c9ed96e1bdd53a976aa1909534606abf05311734c09deb148816458b`; charged delta: `0` lines.
- Harness disposition: `invalidated` because clean ADB preflight found no exact device; no connected instrumentation was launched or retried.
- Process/cleanup: preserved candidate passed focused/full JVM validation and Android-test compilation; no commit, push, PR, review, RDD enablement, relay, ADB override, unrelated-file mutation, or stash mutation occurred.

## PR3 Attempt 6 Status

- Work unit: `pr3-monthly-budget-management`; tasks `3.1`–`3.3` remain unchecked and no implementation is retained.
- Outcome: blocked by the hard 400 changed-line boundary before a coherent slice could be completed; the draft candidate peaked at 713 changed lines (591 tracked plus 122 untracked) and was rolled back.
- TDD evidence: RED was captured with `BudgetViewModelTest` failing to compile against the not-yet-implemented management contracts; no GREEN, task completion, or false success is claimed.
- ADB disposition: the one clean preflight after sourcing the required environment and unsetting `ADB_SERVER_SOCKET`, `ANDROID_ADB_SERVER_ADDRESS`, and `ANDROID_ADB_SERVER_PORT` produced no device listing; no connected instrumentation was run or retried.
- Rollback boundary: all draft PR3 source/test edits were removed; PR1/PR2 history and the protected stash remain unchanged.

## PR3A Status — Atomic Budget Data Mutations

- Work unit: `pr3a-budget-data-mutations`; delivery remains `auto-chain`, `stacked-to-main`.
- Scope is limited to Phase 3A: typed Room/DAO/repository edit and exact delete mutations with focused Room coverage.
- Implemented only exact `(categoryId, YearMonth)` amount replacement/deletion; rollover, ViewModel/Dashboard projection, Compose management UI, dialogs, semantics, and Phases 3B/3C remain out of scope.
- `BudgetTarget` captures the opened amount; `BudgetMutationError`/`BudgetMutationException` distinguish invalid, archived, missing, stale, and unexpected-row outcomes; delete returns `DeleteEvidence(affectedRows)`.
- Both mutations execute under `FinanceDatabase.withTransaction`; update/delete SQL includes the expected opened amount, so a stale target cannot affect another row.
- No entity or schema migration was made.
- Task state: `3A.1`–`3A.3` are complete for this explicitly authorized data-mutation split; `3B.1`–`3C.3` remain unchecked.

## PR3A TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|---|---|---|
| 3A.1–3A.3 | `FinanceDatabaseTest.kt` plus compatible repository fakes | Room instrumentation source + JVM safety net | ✅ Existing Budget/Dashboard ViewModel tests: 7/7 | ✅ Exact edit test was written first; compile failed on missing `BudgetTarget`/`editAmount` | ✅ `compileDebugAndroidTestKotlin`: `BUILD SUCCESSFUL`; runtime execution is unavailable without an exact ADB device | ✅ Exact edit/delete isolation, affected-row evidence, stale/missing typed errors, invalid limit, archived edit rejection, and archived deletion | ✅ Consolidated category lookup; focused compile remained green after refactor |

## PR3A Work Unit Evidence

| Evidence | Result |
|---|---|
| Focused test command and exact result | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && env -u ADB_SERVER_SOCKET -u ANDROID_ADB_SERVER_ADDRESS -u ANDROID_ADB_SERVER_PORT bash gradlew testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest' --no-daemon --rerun-tasks --no-build-cache`: `BUILD SUCCESSFUL`, 7/7. Room-focused `FinanceDatabaseTest` sources compile via `compileDebugAndroidTestKotlin`: `BUILD SUCCESSFUL`. |
| Full JVM suite | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && env -u ADB_SERVER_SOCKET -u ANDROID_ADB_SERVER_ADDRESS -u ANDROID_ADB_SERVER_PORT bash gradlew test --no-daemon --rerun-tasks --no-build-cache`: `BUILD SUCCESSFUL`, 23/23. |
| Runtime harness command/scenario and exact result | Clean preflight `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && env -u ADB_SERVER_SOCKET -u ANDROID_ADB_SERVER_ADDRESS -u ANDROID_ADB_SERVER_PORT timeout 120s adb devices -l` returned no device listing; no exact `device` state was established, so `connectedDebugAndroidTest` was not run or retried. |
| Rollback boundary | Revert only the PR3A hunks in `FinanceRepositories.kt`, `FinanceDao.kt`, `RoomFinanceRepositories.kt`, `FinanceDatabaseTest.kt`, the three repository-fake compatibility edits, `tasks.md` Phase 3A bookkeeping, and this PR3A progress section; no UI, rollover, projection, schema, stash, or unrelated behavior is included. |

## PR3A Files Changed

- `app/src/main/java/com/saldoclaro/finance/domain/repository/FinanceRepositories.kt`
- `app/src/main/java/com/saldoclaro/finance/data/local/FinanceDao.kt`
- `app/src/main/java/com/saldoclaro/finance/data/repository/RoomFinanceRepositories.kt`
- `app/src/androidTest/java/com/saldoclaro/finance/data/local/FinanceDatabaseTest.kt`
- `app/src/androidTest/java/com/saldoclaro/finance/DashboardScreenTest.kt`
- `app/src/test/java/com/saldoclaro/finance/feature/budgets/BudgetViewModelTest.kt`
- `app/src/test/java/com/saldoclaro/finance/feature/dashboard/DashboardViewModelTest.kt`
- `openspec/changes/manage-monthly-budget-limits/tasks.md`

## Native Runtime Attempt 7

- Ordinal: `7`; work unit: `pr3a-budget-data-mutations`; outcome: `passed`; complete: `true`; next action: `complete`.
- Request ID: `d4313205-8c7a-4b94-8e3d-7a4e97cfe352`.
- Expected launch revision: `sha256:8d7245fb1a953180c903fefd89cea3ad727abcb2cd4dc199e6db88a4805028a5`.
- Finish ledger revision: `sha256:f0a088f2c653d523bf431c73494097d9c05d4b21dbeefb058b9656c49d65ef41`.
- Finish candidate identity: `sha256:021c96f66c29c00da8199e10b26156785ca45ded9ce9fc45806d55780aa1d9bc`; candidate tree: `006b7633bcc2913f617a2a940235f59ed90c3398`.
- Evidence revision: `sha256:3f7f29e5e4f678fb5a65bc5754bda8f454810f414deae6ac4fe389faa2d76b9e`; charged delta: `295` lines.
- Harness disposition: `invalidated` because the one clean ADB preflight returned no exact device; connected instrumentation was not launched or retried.
- Process/cleanup: focused/full JVM and Android-test compilation evidence passed; no commit, push, PR, review, RDD enablement, ADB override, relay, stash mutation, or unrelated-file edit occurred; no active native attempt remains.

## Gatekeeper Reconciliation — PR3A

- Task ledger is truthful at `9/15`: tasks `3A.1`–`3A.3` are complete; tasks `3B.1`–`3C.3` remain unchecked.
- PR3A accounting is `371` full changed lines, below the `400`-line review boundary, including the authorized task replan and bookkeeping reconciliation; native charged delta is `295` lines.
- The completed PR3A slice is limited to atomic exact-key Room/DAO/repository edit and delete operations with typed outcomes. It preserves transactions and all other months/categories; rollover, ViewModel/Dashboard projection, and management UI remain in Phases 3B/3C.
- Safety-net result: `BudgetViewModelTest` + `DashboardViewModelTest` passed `7/7`; the full JVM suite passed `23/23`; `compileDebugAndroidTestKotlin` passed.
- Clean ADB preflight found no exact `device`; connected instrumentation was unavailable and was not retried.
- Native authority remains attempt `7`, passed and complete at finish revision `sha256:f0a088f2c653d523bf431c73494097d9c05d4b21dbeefb058b9656c49d65ef41`, evidence revision `sha256:3f7f29e5e4f678fb5a65bc5754bda8f454810f414deae6ac4fe389faa2d76b9e`.
- Rollback boundary: revert only the PR3A hunks in `FinanceRepositories.kt`, `FinanceDao.kt`, `RoomFinanceRepositories.kt`, `FinanceDatabaseTest.kt`, the three repository-fake compatibility edits, Phase 3A bookkeeping, and this PR3A progress section; do not remove UI, rollover, projection, schema, stash, or unrelated behavior.
- This reconciliation changed only SDD artifacts and Engram bookkeeping; it did not modify source/test code, run tests/builds, mutate Git/stash, start/reset/finish runtime attempts, invoke review, or enable RDD.

## PR3B Work Unit Evidence — `3B.1`–`3B.3` complete; PR3C / 3C.1 next; PR3C Compose UI/semantics out of scope.
### TDD Cycle Evidence
| Task | RED | GREEN | TRIANGULATE | REFACTOR |
|---|---|---|---|---|
| 3B.1 | ✅ missing source/state/rollover symbols | ✅ focused 10/10 | ✅ rollover, active/archived, stale, no-limit | ✅ focused 10/10 |
| 3B.2 | ✅ new month source seam | ✅ 10/10 + compile | ✅ both ViewModels switch months | ✅ no Compose management changes |
| 3B.3 | ✅ assertions preceded production | ✅ full JVM 26/26 | ✅ exact-device 15/15 | ✅ `git diff --check` |
### Work Unit Evidence
- Focused tests: `bash gradlew testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest'` → 10/10, `BUILD SUCCESSFUL`; full JVM `bash gradlew test` → 26/26, `BUILD SUCCESSFUL`; Android-test compilation `bash gradlew compileDebugAndroidTestKotlin` → `BUILD SUCCESSFUL`.
- Runtime harness: one clean env/unset ADB preflight found `c0e19fe4 device`; `bash gradlew connectedDebugAndroidTest` → 15/15, `BUILD SUCCESSFUL`.
- Rollback boundary: revert PR3B hunks in `CurrentMonthSource.kt`, both ViewModels, rollover DAO/repository, `AppContainer.kt`/`SaldoClaroNavHost.kt`, focused tests, Dashboard expectation, and PR3B artifacts; preserve PR3A/Spanish history.
