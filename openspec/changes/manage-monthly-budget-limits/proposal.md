# Proposal: Manage Monthly Budget Limits

## Intent

Enable current-month limit management and make Saldo Claro Spanish-only while preserving data and technical contracts.

## Scope

### In Scope
- Open management from a budget card; edit only its amount.
- Confirm exact deletion without affecting transactions or other months.
- Keep post-delete spending visible without a limit on both screens.
- Refresh automatically at month rollover; reject stale targets.
- Permit archived-category deletion, never editing.
- Translate all visible and accessibility copy, states, dialogs, and errors.
- Resource Spanish copy, dates, currency, and built-in labels; preserve user category names.

### Out of Scope
- Reassignment; historical/future management; recurrence, undo, audit history, soft deletion, or migration.
- Category restoration or transaction behavior changes.
- Locale adaptation, language selection, locale variants, or active English fallback.
- Translating identifiers, persisted keys, logs, test names, or SDD artifacts.

## Capabilities

### New Capabilities
- `monthly-budget-management`: Amount editing, exact deletion, post-delete visibility, archived-category rules, and rollover.
- `spanish-application-presentation`: Spanish-only app copy, accessibility, errors, formatting, and built-in labels.

### Modified Capabilities
- None; `openspec/specs/` has no existing capability specifications.

## Approach

Retain `(categoryId, YearMonth)`, reuse upsert, and add exact deletion with explicit mutation state and clock-aware observation. Centralize default Spanish resources, typed UI errors, fixed Spanish formatting, and stable-ID built-in labels. Deliver focused `auto-chain` slices.

## Affected Areas

| Area | Impact | Description |
|---|---|---|
| `feature/budgets/`, `domain/repository/`, `data/` | Modified | Budget management |
| `navigation/`, `core/`, `feature/` | Modified | Spanish presentation |
| `app/src/main/res/values/strings.xml` | Modified | Spanish resource catalog |
| `app/src/test/`, `app/src/androidTest/` | Modified/New | Behavioral and presentation proof |

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Wrong-month deletion or stale mutation | Medium | Require exact keys and validate rollover targets |
| English leaks from errors or legacy built-ins | High | Typed messages and stable-ID label mapping |
| Combined 660–850 lines overload review | High | Chained work units under `auto-chain` |

## Rollback Plan

Revert budget behavior, then Spanish presentation slices. No migration or key conversion affects stored data.

## Dependencies

- Existing Room flows, clock/zone, Android resources, and formatting support.
- Reconcile config's stale `English UI copy` convention before design/tasks.

## Success Criteria

- [ ] Mutations affect only the target; transactions and other months remain unchanged.
- [ ] Visibility, rollover, stale-target, and archived-category rules pass tests.
- [ ] Audits find zero app-authored English in UI or accessibility states.
- [ ] Dates/currency remain Spanish regardless of device locale.
- [ ] Built-ins display Spanish for new and legacy rows; user names remain unchanged.
- [ ] Persisted keys remain unchanged; no selector or English fallback exists.
