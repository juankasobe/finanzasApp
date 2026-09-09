# Monthly Budget Management Specification

## Purpose

Define current-month limit management that preserves history and cross-screen accuracy.

## Requirements

### Requirement: Card-initiated management

The system MUST open a separate management surface when a current-month limit card is tapped.

#### Scenario: Open selected limit

- GIVEN a current-month card with a limit
- WHEN the user taps the card
- THEN management shows its category, month, and amount

#### Scenario: Card has spending but no limit

- GIVEN a card represents current-month spending without a limit
- WHEN the user taps the card
- THEN edit and delete actions MUST NOT be offered

### Requirement: Amount-only editing

The system MUST edit only the selected amount and MUST keep category and month fixed.

#### Scenario: Edit amount

- GIVEN an active category limit is open
- WHEN the user submits a valid replacement amount
- THEN only that category-month amount changes

#### Scenario: Invalid amount

- GIVEN a current-month limit is open
- WHEN the user submits an invalid or non-positive amount
- THEN the edit is rejected and the limit preserved

### Requirement: Confirmed exact deletion

The system MUST confirm deletion and remove only the selected current-month/category limit, preserving transactions and other months.

#### Scenario: Cancel deletion

- GIVEN deletion confirmation is displayed
- WHEN the user cancels
- THEN no limit or transaction is changed

#### Scenario: Confirm deletion

- GIVEN the category has limits in multiple months
- WHEN the user confirms deletion of the current-month limit
- THEN only the current-month/category limit is removed
- AND all transactions and other-month limits remain unchanged

### Requirement: No-limit spending visibility

After deletion, Budgets and Dashboard MUST show remaining current-month category spending with no monthly limit.

#### Scenario: Spending remains after deletion

- GIVEN a deleted limit's category has current-month spending
- WHEN Budgets and Dashboard reflect the deletion
- THEN both surfaces show that spending with no monthly limit

#### Scenario: No spending remains

- GIVEN a deleted limit's category has no current-month spending
- WHEN the deletion is reflected
- THEN the system MAY omit that category from no-limit summaries

### Requirement: Automatic month rollover

The system MUST change management to the new month at rollover without restarting.

#### Scenario: Month changes while running

- GIVEN the application remains running across a month boundary
- WHEN the current month changes
- THEN cards and management targets refresh to the new month

#### Scenario: Prior-month surface remains open

- GIVEN a management surface was opened before rollover
- WHEN the month changes before mutation
- THEN the system rejects mutation of the prior-month target and refreshes or closes the surface

### Requirement: Archived-category restrictions

The system MUST allow deleting, but MUST NOT allow editing, an archived category's current-month limit.

#### Scenario: Manage archived category

- GIVEN an archived category has a current-month limit
- WHEN its management surface opens
- THEN deletion is available and editing is unavailable

#### Scenario: Archived edit attempt

- GIVEN an archived category's limit is targeted for editing
- WHEN an edit is submitted
- THEN the system rejects it and preserves the limit

### Requirement: Truthful mutation outcomes

The system MUST report success only after the target changes; failures and missing or stale targets MUST NOT produce false success.

#### Scenario: Mutation fails

- GIVEN an edit or confirmed deletion is requested
- WHEN the mutation fails
- THEN the system shows an error and keeps the management context available for recovery
- AND it MUST NOT report success

#### Scenario: Target is missing or stale

- GIVEN the selected limit was removed or changed after the surface opened
- WHEN the user submits an edit or deletion
- THEN the system reports that the target is unavailable or stale
- AND it MUST NOT mutate another limit or report success
