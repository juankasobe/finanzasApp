# Spanish Application Presentation Specification

## Purpose

Define a Spanish-only application presentation while preserving user data and technical contracts.

## Requirements

### Requirement: Spanish app-authored presentation

Every app-authored visible string and accessibility or content description MUST be Spanish.

#### Scenario: Navigate and act

- GIVEN any application destination
- WHEN navigation, actions, or success feedback are shown
- THEN all app-authored visible and accessibility-facing copy is Spanish

#### Scenario: Show transient and terminal states

- GIVEN a surface enters loading, empty, validation, error, or retry state
- WHEN the state is presented
- THEN every app-authored message and action is Spanish

### Requirement: Spanish dialogs and confirmations

Dialogs and confirmations MUST present their title, message, actions, and accessibility descriptions in Spanish.

#### Scenario: Confirm an action

- GIVEN an action requires confirmation
- WHEN its dialog opens
- THEN the dialog and confirm and cancel actions are Spanish

#### Scenario: Dialog reports an error

- GIVEN an operation fails while a dialog or management surface is active
- WHEN recovery is offered
- THEN the error and retry action are Spanish

### Requirement: Deterministic Spanish formatting

App-authored dates and currency MUST use consistent Spanish presentation regardless of device locale.

#### Scenario: Device locale differs

- GIVEN the device locale is not Spanish
- WHEN an app-authored date or currency value is displayed
- THEN its formatting remains Spanish

#### Scenario: Device locale is Spanish

- GIVEN the device locale is Spanish
- WHEN equivalent values are displayed across destinations
- THEN dates and currency use the same Spanish conventions

### Requirement: Spanish built-in category labels

Built-in categories MUST display app-authored Spanish labels based on their stable identity.

#### Scenario: Display a built-in category

- GIVEN a current built-in category row exists
- WHEN its label appears on any surface
- THEN the Spanish app-authored label is shown consistently

#### Scenario: Display a legacy English row

- GIVEN a persisted built-in row retains an English name
- WHEN its stable built-in identity is presented
- THEN the Spanish app-authored label is shown instead of the persisted English name

### Requirement: Preserve user-entered data

The system MUST display user-entered names and data unchanged and MUST NOT translate them.

#### Scenario: User data resembles English copy

- GIVEN a user entered an English or mixed-language name
- WHEN that value is displayed
- THEN the exact user-entered value remains unchanged

#### Scenario: User data accompanies Spanish copy

- GIVEN user-entered data appears within an app-authored presentation
- WHEN the presentation is shown
- THEN surrounding app-authored copy is Spanish and the user data is unchanged

### Requirement: Safe UI error boundary

UI copy MUST NOT expose raw exception messages, technical identifiers, persisted keys, logs, or test names.

#### Scenario: Unexpected failure

- GIVEN a failure contains a raw or non-Spanish technical message
- WHEN the failure reaches a user-facing state
- THEN a Spanish app-authored error is shown without the raw message

#### Scenario: Technical values remain internal

- GIVEN identifiers, keys, logs, or test names are used internally
- WHEN the application presents related behavior
- THEN those values are neither exposed nor translated as UI copy

### Requirement: Spanish-only language policy

The application MUST NOT provide a language selector, device-locale adaptation, locale variants, or an active English fallback.

#### Scenario: Unsupported device locale

- GIVEN the device uses any locale
- WHEN the application starts or the locale changes
- THEN app-authored presentation remains Spanish without a language choice

#### Scenario: Spanish copy is unavailable

- GIVEN a user-facing state requires app-authored copy
- WHEN that state is rendered
- THEN the system MUST NOT substitute active English fallback copy

### Requirement: Spanish exact-text contracts

Exact-text UI and accessibility contracts MUST assert the required Spanish presentation without weakening semantic coverage.

#### Scenario: Visible exact-text contract

- GIVEN an automated contract selects app-authored visible copy
- WHEN the corresponding state is tested
- THEN its expected text is the Spanish product text

#### Scenario: Accessibility exact-text contract

- GIVEN an automated contract selects a content description or accessibility label
- WHEN the corresponding control or status is tested
- THEN its expected description is Spanish and identifies the same behavior
