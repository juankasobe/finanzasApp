```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:8cf0cd3be3e4399a3c5990003bc3edbfdd5b10d84c4ef7e74682c42429a75cca
verdict: pass_with_warnings
blockers: 0
critical_findings: 0
requirements: 15/15
scenarios: 30/30
test_command: "source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest' --tests '*UiPresentationTest' --tests '*TransactionViewModelTest' --no-daemon --rerun-tasks --no-build-cache"
test_exit_code: 0
test_output_hash: sha256:95878cd8bb3a24fd86919666a9f301c47eca3817215026b9162020a6f7136678
build_command: "source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew assembleDebug --no-daemon --rerun-tasks --no-build-cache"
build_exit_code: 0
build_output_hash: sha256:5ecb66089d166399817a3bcfb4057a68eb286bfb2335fb385659bb2a576aaf9f
```

## Verification Report

**Change**: `manage-monthly-budget-limits`  
**Version**: N/A  
**Mode**: Strict TDD  
**Revision under test**: working tree and index on `feat/budget-management-ui`; base `HEAD` `db862bdaf32002779d2fe5354270300888726595`  
**Artifact store**: Hybrid (OpenSpec file plus Engram topic)  
**Native status**: `gentle-ai.sdd-status` v2; tasks `15/15`; apply `all_done`; verify `ready`; archive `blocked`  
**Context read directly**: proposal, both capability specs, design, tasks, apply-progress, prior verify report, implementation, and unit/Android test sources.  
**Freshness**: all required commands in this report were executed during this verification; prior Android results were not reused.  

### Completeness and Candidate Scope

| Metric | Result |
|---|---:|
| Tasks | 15/15 complete; 0 pending |
| Requirements | 15/15 |
| Scenarios | 30/30 |
| Tracked candidate changed lines | 400/400 (377 additions, 23 deletions) |
| Tracked candidate changed files | 8 |
| Coverage | Unavailable by project configuration |

The retrieved specifications contain 7 monthly-budget requirements plus 8 Spanish-presentation requirements, and 14 plus 16 scenarios respectively. All native task checkboxes are complete. The tracked candidate remains at the supplied 400-line boundary; the pre-existing untracked verify report is excluded from that tracked candidate measurement.

### Build, Test, and Audit Execution

| Evidence | Exact command/result | Exit | Output hash |
|---|---|---:|---|
| Focused JVM | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest' --tests '*UiPresentationTest' --tests '*TransactionViewModelTest' --no-daemon --rerun-tasks --no-build-cache` — 21/21 passed (Budget 5, Dashboard 5, presentation 4, Transaction 7) | 0 | `sha256:95878cd8bb3a24fd86919666a9f301c47eca3817215026b9162020a6f7136678` |
| Full JVM | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew test --no-daemon --rerun-tasks --no-build-cache` — 26/26 passed | 0 | `sha256:6188a80698502d35adac48898c98ca554565dc8409f1be08d9a2fa5c75f7d64f` |
| Android-test compilation | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew compileDebugAndroidTestKotlin --no-daemon --rerun-tasks --no-build-cache` — `BUILD SUCCESSFUL` | 0 | `sha256:01460a04d37058248b736147c5995bf065700b784f9b6463d9a19aeaa4b1ec19` |
| Debug build | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew assembleDebug --no-daemon --rerun-tasks --no-build-cache` — `BUILD SUCCESSFUL` | 0 | `sha256:5ecb66089d166399817a3bcfb4057a68eb286bfb2335fb385659bb2a576aaf9f` |
| Main Kotlin compilation | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew compileDebugKotlin --no-daemon --rerun-tasks --no-build-cache` — `BUILD SUCCESSFUL` | 0 | `sha256:e499b1d81be76471faa184ea3421a1fecfb4c23495f31de484ed47508d206240` |
| Lint | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew lintDebug --no-daemon --rerun-tasks --no-build-cache` — `BUILD SUCCESSFUL`; AGP configuration warnings only | 0 | `sha256:eb2283625b3a6ba90ba0d64c3fddcb152bf8d18e2741bb1301cc160a9d9833e9` |
| Exact-device preflight | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && timeout 120s adb devices -l` — serial `c0e19fe4` in exact state `device`; product `flourite_global`, model `2510ERA8BG`, Android 16 | 0 | `sha256:24485523e6ff853619dba37c7cbfe46c15dab1bf1f147d1c1d5c5cbeba27e478` |
| Connected instrumentation | `source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew connectedDebugAndroidTest --no-daemon --rerun-tasks --no-build-cache` — 20/20 passed, 0 failed, 0 skipped, on `c0e19fe4` | 0 | `sha256:8002b7e9ad8e4997b807d08a293b9c32e5db3166344d79ef2e0562cc5c9625bf` |
| Spanish/resource/accessibility audit and diff hygiene | Default-only resources, 104 non-empty default strings, no alternate locale directory, no direct production `Text` literals, no literal content descriptions, no raw exception-message use, no locale selector/fallback markers; `git diff --check` and `git diff HEAD --check` both clean | 0 | `sha256:a34d43494bd611087e12c28b4cf9c0cdd9c7196ae814042a67b2b8991496ee59` |

The connected result XML independently records 20 tests with 0 failures, 0 errors, and 0 skips: 12 Compose tests (`AppSemanticsTest` 3, `BudgetScreenTest` 5, `DashboardScreenTest` 4) and 8 Room tests (`FinanceDatabaseTest`). Gradle output contains non-blocking AGP deprecation/configuration warnings, Kotlin experimental-API warnings, Compose deprecation warnings, and an unstrippable native library note; none caused a non-zero exit.

### MB-5b Explicit Verification

`BudgetViewModelTest.month refresh rolls active limits without replacing current or archived budgets` opens a current-month `BudgetTarget`, advances and refreshes the injected month source, and asserts `BudgetMutationState.Idle` before checking the new-month projection. It passed in the focused 5/5 JVM run. In production, `BudgetViewModel.observeMonth` clears `pendingMutation` and assigns `BudgetMutationState.Idle` when the emitted month differs from the observed month, before invoking `budgetRepository.rollover` and before the new month flows emit. The stale-target guard still rejects any late mutation attempt. This directly covers the remediation requirement without an implementation change during verification.

### Spec Compliance Matrix

A scenario is compliant here only where the named covering test passed in the current JVM or connected runtime. Static audit evidence is supplementary for policy and resource invariants.

| ID | Requirement / scenario | Current runtime covering test | Result |
|---|---|---|---|
| MB-1a | Card-initiated management — a limited current-month card opens category, month, and amount | `BudgetScreenTest.limitCardOpensExactTargetAndValidEditChangesOnlyItsAmount` (Compose, 20/20) | COMPLIANT |
| MB-1b | Card-initiated management — a no-limit spending card offers no edit/delete actions | `BudgetScreenTest.spendingWithoutLimitRemainsVisibleWithoutManagementAction` (Compose, 20/20) | COMPLIANT |
| MB-2a | Amount-only editing changes only the selected category/month amount | `BudgetScreenTest.limitCardOpensExactTargetAndValidEditChangesOnlyItsAmount`; `FinanceDatabaseTest.editingBudgetChangesOnlyTheOpenedCategoryMonthAndPreservesTransactions` (Compose + Room, 20/20) | COMPLIANT |
| MB-2b | Invalid/non-positive edit is rejected and preserves the limit | `BudgetScreenTest.invalidEditAndDeleteCancellationPreserveDataThenConfirmedDeleteKeepsSpending`; `BudgetScreenTest.invalidEditKeepsManagementContextAvailableForCorrection` (Compose, 20/20) | COMPLIANT |
| MB-3a | Canceling deletion changes neither limit nor transaction | `BudgetScreenTest.invalidEditAndDeleteCancellationPreserveDataThenConfirmedDeleteKeepsSpending` (Compose, 20/20) | COMPLIANT |
| MB-3b | Confirmed deletion removes only the exact current category/month limit | `BudgetScreenTest.invalidEditAndDeleteCancellationPreserveDataThenConfirmedDeleteKeepsSpending`; `FinanceDatabaseTest.deletingBudgetReturnsAffectedRowAndLeavesOtherMonthsCategoriesAndTransactions` (Compose + Room, 20/20) | COMPLIANT |
| MB-4a | Spending after deletion remains visible without a limit in Budgets and Dashboard | `BudgetScreenTest.invalidEditAndDeleteCancellationPreserveDataThenConfirmedDeleteKeepsSpending`; `DashboardScreenTest.dashboardExposesNoBudgetOverviewForCurrentMonthActivity` (Compose, 20/20) | COMPLIANT |
| MB-4b | A deleted category with no remaining spending may be omitted from no-limit summaries | `DashboardViewModelTest.dashboard stays loading until both current month sources emit` asserts the empty union resolves to `NoBudgets`; `BudgetScreen` empty projection path is compiled and connected-tested (JVM + Compose, current runs) | COMPLIANT |
| MB-5a | Running across a month boundary switches current-month projections | `BudgetViewModelTest.month refresh rolls active limits without replacing current or archived budgets`; `DashboardViewModelTest.month refresh switches dashboard projection to the new local month` (JVM, 26/26) | COMPLIANT |
| MB-5b | An open prior-month management surface is rejected and refreshed or closed after rollover | `BudgetViewModelTest.month refresh rolls active limits without replacing current or archived budgets` (JVM, focused 5/5; explicit `Idle` assertion) | COMPLIANT |
| MB-6a | Archived current-month limit exposes delete-only management | `BudgetScreenTest.archivedLimitOffersDeleteWithoutEditAffordance` (Compose, 20/20) | COMPLIANT |
| MB-6b | Archived edit is rejected and the limit is preserved | `BudgetViewModelTest.archived edit fails delete succeeds and stale target remains recoverable`; `FinanceDatabaseTest.invalidAndArchivedEditsAreRejectedButArchivedDeletionSucceeds` (JVM + Room, current runs) | COMPLIANT |
| MB-7a | Mutation failure shows recoverable context and no false success | `BudgetViewModelTest.archived edit fails delete succeeds and stale target remains recoverable`; `DashboardScreenTest.retryClearsStaleDashboardValuesAndWaitsForFreshSnapshots` (JVM + Compose, current runs) | COMPLIANT |
| MB-7b | Missing/stale target reports unavailable and cannot mutate another limit | `BudgetViewModelTest.archived edit fails delete succeeds and stale target remains recoverable`; `FinanceDatabaseTest.staleAndMissingTargetsReturnTypedErrorsWithoutChangingExistingLimits` (JVM + Room, current runs) | COMPLIANT |
| SP-1a | Navigate and act — app-authored visible/accessibility copy is Spanish | `AppSemanticsTest.appOffersOnlyApprovedOfflineDestinationsThroughAccessibleLabels` (Compose, 20/20) | COMPLIANT |
| SP-1b | Loading, empty, validation, error, and retry states are Spanish | `DashboardScreenTest.dashboardAnnouncesLoadingBeforeBothMonthSnapshots`; `DashboardScreenTest.retryClearsStaleDashboardValuesAndWaitsForFreshSnapshots`; focused validation/error tests (current JVM + Compose runs) | COMPLIANT |
| SP-2a | Confirmation dialogs expose Spanish title, message, actions, and semantics | `BudgetScreenTest.invalidEditAndDeleteCancellationPreserveDataThenConfirmedDeleteKeepsSpending` (Compose, 20/20) | COMPLIANT |
| SP-2b | Dialog operation failure exposes Spanish recovery copy | `DashboardScreenTest.retryClearsStaleDashboardValuesAndWaitsForFreshSnapshots` (Compose, 20/20) | COMPLIANT |
| SP-3a | Dates/currency stay Spanish when device locale differs | `UiPresentationTest.currency and date use fixed Spanish presentation regardless of default locale` (JVM, focused 21/21) | COMPLIANT |
| SP-3b | Equivalent values use the same Spanish conventions on a Spanish device locale | `UiPresentationTest.currency and date use fixed Spanish presentation regardless of default locale` (JVM, focused 21/21) | COMPLIANT |
| SP-4a | Current built-in identity displays its Spanish label | `UiPresentationTest.current and legacy built in ids use Spanish labels`; `FinanceDatabaseTest.builtInCategoriesAreSeededWithStableIds` (JVM + Room, current runs) | COMPLIANT |
| SP-4b | Legacy English persisted built-in name is replaced by the Spanish identity label | `UiPresentationTest.current and legacy built in ids use Spanish labels` (JVM, focused 21/21) | COMPLIANT |
| SP-5a | English or mixed-language custom names remain unchanged | `UiPresentationTest.custom category names remain unchanged` (JVM, focused 21/21) | COMPLIANT |
| SP-5b | User data remains unchanged while surrounding app-authored copy is Spanish | `UiPresentationTest.custom category names remain unchanged`; connected Spanish exact-text contracts plus resource audit (JVM + Compose, current runs) | COMPLIANT |
| SP-6a | Unexpected raw/non-Spanish failure reaches a safe Spanish UI error | `TransactionViewModelTest.read failure exposes a safe data error instead of its raw message`; `DashboardScreenTest.retryClearsStaleDashboardValuesAndWaitsForFreshSnapshots` (JVM + Compose, current runs) | COMPLIANT |
| SP-6b | Technical identifiers, keys, logs, and test names remain internal | `DashboardScreenTest.retryClearsStaleDashboardValuesAndWaitsForFreshSnapshots` explicitly rejects the raw failure; static audit also passes (Compose, current run) | COMPLIANT |
| SP-7a | Unsupported device locale keeps Spanish copy without language choice | `AppSemanticsTest.applicationKeepsSpanishCopyWhenDeviceUsesUnsupportedLocale` (Compose, 20/20) | COMPLIANT |
| SP-7b | Missing Spanish copy never activates an English fallback | `AppSemanticsTest.applicationKeepsSpanishCopyWhenDeviceUsesUnsupportedLocale` plus current default-only resource/fallback audit (Compose + static audit) | COMPLIANT |
| SP-8a | Visible exact-text contracts assert Spanish product text | `AppSemanticsTest`, `DashboardScreenTest`, and `BudgetScreenTest` exact visible assertions (Compose, 12/12) | COMPLIANT |
| SP-8b | Accessibility exact-text contracts assert Spanish behavior descriptions | `AppSemanticsTest` and `BudgetScreenTest` exact content-description assertions (Compose, 12/12) | COMPLIANT |

**Compliance summary**: 30/30 scenarios compliant in this independent run; 0 failing and 0 untested.

### Correctness (Static and Runtime Evidence)

| Requirement group | Status | Evidence |
|---|---|---|
| Card management and amount-only editing | Implemented and runtime-covered | Immutable `BudgetTarget`, separate management dialog, exact current-month card semantics, validation, and connected Compose edit flows pass. |
| Exact deletion and preservation | Implemented and runtime-covered | Room transaction, exact category/month/opened-amount predicates, affected-row evidence, and connected Room/Compose deletion flows pass. |
| No-limit projection | Implemented and runtime-covered | Expense/limit union feeds Budgets and Dashboard; current no-limit Compose and JVM projections pass. |
| Rollover and stale management state | Implemented and runtime-covered | Shared clock/zone source switches both projections; `BudgetViewModel` resets open mutation state before rollover and rejects stale late mutations. |
| Archived-category restrictions and truthful outcomes | Implemented and runtime-covered | Archived edit/delete rules, typed target errors, recoverable mutation state, and Room isolation tests pass. |
| Spanish presentation and formatting | Implemented and runtime-covered | Default resource catalog, typed errors, fixed `es-ES` formatters, stable built-in IDs, exact Compose text, and accessibility contracts pass. |
| Language and data-preservation policy | Implemented and audit-covered | User names are preserved, no locale variants/selectors/fallback markers exist, and no raw exception message reaches production UI. |

### Design Coherence

| Design decision | Result | Evidence |
|---|---|---|
| `BudgetMutationState` owns selected snapshot, confirmation, running, validation, and recoverable error | Followed | `BudgetViewModel` and `BudgetManagementDialog` retain the target and distinguish edit/delete recovery. |
| Exact `(categoryId, YearMonth)` mutation with opened-amount comparison | Followed | DAO predicates and `RoomBudgetRepository` transaction guards pass Room tests. |
| Shared clock/zone month source with foreground refresh | Followed | `AppContainer`, navigation lifecycle, and both month-aware ViewModels share `CurrentMonthSource`. |
| Rollover avoids archived copies and destination duplicates | Followed | Repository rollover checks category archive state and destination keys. |
| Default Spanish resources, typed errors, fixed formatters, and stable built-in labels | Mostly followed | Compose is resource-backed; the pure two-argument `categoryPresentationName` convenience overload contains Spanish literals as a non-visible fallback. |
| Dashboard/Budgets consume the transaction/budget union | Followed | Both ViewModels project current-month expenses and budgets through `projectBudgetProgress`. |
| Open stale management surface closes or refreshes on rollover | Followed after remediation | Month-change handling clears the mutation state to `Idle` before rollover work; MB-5b current focused test passes. |

### TDD Compliance

| Check | Result | Details |
|---|---|---|
| TDD evidence reported | PASS | `apply-progress.md` contains TDD Cycle Evidence tables for presentation, translated UI, data mutations, rollover, Compose management, and MB-5b remediation. |
| All task test files exist | PASS | All listed unit, Room, and Compose test files exist; current JVM and connected suites pass. |
| RED confirmed | PASS | All 15 completed task rows have recorded RED evidence or an explicit behavior-preserving refactor rationale. |
| GREEN confirmed | PASS | Current focused JVM 21/21, full JVM 26/26, Android-test compilation, and connected 20/20 pass. |
| Triangulation | PASS WITH WARNING | Apply evidence records varied inputs and outcomes; some task rows aggregate multiple scenarios rather than one test per scenario. |
| Safety net | PASS WITH LIMITATION | Historical safety-net evidence is recorded in apply-progress; this final run independently reran all applicable JVM/build/runtime commands. |

**TDD Compliance**: 4/6 checks pass directly; 2 pass with non-blocking documentation limitations.

### Test Layer Distribution

| Layer | Tests | Files | Tools/result |
|---|---:|---:|---|
| Unit/JVM | 26 | 5 | JUnit 4 via Gradle; 26/26 passed |
| Android integration — Compose/UI | 12 | 3 | AndroidJUnit4 + Compose UI test; 12/12 passed on `c0e19fe4` |
| Android integration — Room | 8 | 1 | AndroidJUnit4 + in-memory Room; 8/8 passed on `c0e19fe4` |
| E2E | 0 | 0 | Not applicable |
| **Total** | **46** | **9** | **All current runtime tests passed** |

### Changed-File Coverage

Coverage analysis skipped — project configuration declares coverage unavailable. No coverage percentage is claimed.

### Assertion Quality

| File | Lines | Assertion pattern | Issue | Severity |
|---|---:|---|---|---|
| `app/src/androidTest/java/com/saldoclaro/finance/BudgetScreenTest.kt` | 36, 48, 56, 60 | Fake repository call-count assertions | Useful side-effect guards but coupled to fake implementation details; observable state/data assertions accompany them. | WARNING |
| `app/src/test/java/com/saldoclaro/finance/feature/budgets/BudgetViewModelTest.kt` | 158 | Fake repository `editCalls` assertion | Side-effect guard is implementation-coupled; observable mutation state and data assertions accompany it. | WARNING |

No tautologies, ghost loops, orphan empty assertions without non-empty companions, assertion-free production paths, or smoke-test-only cases were found.

**Assertion quality**: 0 CRITICAL, 2 WARNING.

### Quality Metrics

- **Type checker**: PASS — `compileDebugKotlin` exit 0.
- **Linter**: PASS WITH WARNING — `lintDebug` exit 0; AGP configuration/deprecation warnings only.
- **Formatter**: Not available by project configuration.
- **Diff hygiene**: PASS — `git diff --check` and `git diff HEAD --check` exit 0.

### Issues Found

**CRITICAL**: None.  
**WARNING**:

1. The pure two-argument `UiPresentation.categoryPresentationName` convenience overload embeds Spanish fallback literals instead of resolving the default resource catalog; this does not create an English or visible-runtime leak.
2. Gradle emitted non-blocking AGP configuration/deprecation, Kotlin experimental-API, Compose deprecation, and native-library packaging warnings.
3. Two test files use fake repository call-count assertions in addition to observable behavior assertions.
4. Coverage and formatter tooling are unavailable by project configuration.

**SUGGESTION**:

1. Prefer resource resolution in the non-Compose presentation convenience overload to keep all app-authored copy in the catalog.
2. If future verification needs finer TDD attribution, split aggregate task tests into scenario-named cases while retaining the current behavioral coverage.

### Verdict

**PASS WITH WARNINGS** — all 15 requirements and 30 scenarios have current passing runtime coverage; focused/full JVM, Android-test compilation, debug build, main compilation, lint, exact-device preflight, full Compose/Room instrumentation, Spanish audit, and diff hygiene all passed. Warnings are non-blocking and do not suppress parent settlement.

### Canonical Verification Evidence

The `evidence_revision` is the SHA-256 of the following exact UTF-8 preimage (47 lines, trailing newline included):

```text
change=manage-monthly-budget-limits
workspace=/home/juanka/dev/finanzasApp
branch=feat/budget-management-ui
base=HEAD@db862bdaf32002779d2fe5354270300888726595
candidate=working-tree-and-index;tracked_changed_files=8;tracked_changed_lines=400/400
tasks=15/15
requirements=15/15
scenarios=30/30
focused_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew testDebugUnitTest --tests '*BudgetViewModelTest' --tests '*DashboardViewModelTest' --tests '*UiPresentationTest' --tests '*TransactionViewModelTest' --no-daemon --rerun-tasks --no-build-cache
focused_exit=0
focused_result=21/21
focused_output_hash=sha256:95878cd8bb3a24fd86919666a9f301c47eca3817215026b9162020a6f7136678
full_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew test --no-daemon --rerun-tasks --no-build-cache
full_exit=0
full_result=26/26
full_output_hash=sha256:6188a80698502d35adac48898c98ca554565dc8409f1be08d9a2fa5c75f7d64f
android_test_compile_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew compileDebugAndroidTestKotlin --no-daemon --rerun-tasks --no-build-cache
android_test_compile_exit=0
android_test_compile_result=BUILD_SUCCESSFUL
android_test_compile_output_hash=sha256:01460a04d37058248b736147c5995bf065700b784f9b6463d9a19aeaa4b1ec19
assemble_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew assembleDebug --no-daemon --rerun-tasks --no-build-cache
assemble_exit=0
assemble_result=BUILD_SUCCESSFUL
assemble_output_hash=sha256:5ecb66089d166399817a3bcfb4057a68eb286bfb2335fb385659bb2a576aaf9f
compile_main_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew compileDebugKotlin --no-daemon --rerun-tasks --no-build-cache
compile_main_exit=0
compile_main_result=BUILD_SUCCESSFUL
compile_main_output_hash=sha256:e499b1d81be76471faa184ea3421a1fecfb4c23495f31de484ed47508d206240
lint_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew lintDebug --no-daemon --rerun-tasks --no-build-cache
lint_exit=0
lint_result=BUILD_SUCCESSFUL;AGP_configuration_warnings_only
lint_output_hash=sha256:eb2283625b3a6ba90ba0d64c3fddcb152bf8d18e2741bb1301cc160a9d9833e9
adb_preflight_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && timeout 120s adb devices -l
adb_preflight_exit=0
adb_preflight_result=serial=c0e19fe4;state=device;product=flourite_global;model=2510ERA8BG;android=16
adb_preflight_output_hash=sha256:24485523e6ff853619dba37c7cbfe46c15dab1bf1f147d1c1d5c5cbeba27e478
connected_command=source /home/juanka/.local/share/finanzasapp-android-validation/environment.sh && unset ADB_SERVER_SOCKET ANDROID_ADB_SERVER_ADDRESS ANDROID_ADB_SERVER_PORT ANDROID_SERIAL && bash gradlew connectedDebugAndroidTest --no-daemon --rerun-tasks --no-build-cache
connected_exit=0
connected_result=20/20;compose=12/12;room=8/8;failures=0;skipped=0;device=c0e19fe4
connected_output_hash=sha256:8002b7e9ad8e4997b807d08a293b9c32e5db3166344d79ef2e0562cc5c9625bf
resource_audit=default_values_only;no_alternate_locale;no_direct_production_ui_literals;no_literal_content_descriptions;no_raw_exception_disclosure;no_selector_or_fallback_markers;git_diff_check=0;git_diff_head_check=0
resource_audit_output_hash=sha256:a34d43494bd611087e12c28b4cf9c0cdd9c7196ae814042a67b2b8991496ee59
mb5b=BudgetViewModelTest.month_refresh_rolls_active_limits..._asserts_mutation_Idle;BudgetViewModel.observeMonth_clears_pending_mutation_before_rollover
coverage=unavailable
strict_tdd=true
critical_findings=0
verdict=pass_with_warnings
```
