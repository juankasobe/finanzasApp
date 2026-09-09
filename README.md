# Saldo Claro: understand your month at a glance

Saldo Claro is an offline Android finance tracker for people who want a clear view of their current-month income, expenses, and spending limits. It records amounts in USD, presents its entire interface in Spanish, and keeps its data on the device.

## What you can do

- Review current-month income, expenses, balance, recent activity, and budget progress from one dashboard.
- Record and delete income or expense transactions with an amount, category, and date.
- Create custom categories and archive those no longer used while retaining their historical records.
- Create, edit, and delete per-category limits for the current month.
- Compare category spending with its limit and see under-limit, at-limit, over-limit, and no-limit states.
- Carry active budget limits into a new month without overwriting existing monthly limits or historical data.

## Tech stack

| Area | Technology |
|---|---|
| Language and UI | Kotlin 2.3.20, Jetpack Compose, Material 3 |
| Navigation and state | Navigation Compose, ViewModels, coroutines, and `StateFlow` |
| Persistence | Room 2.8.4 with KSP-generated code and versioned schema export |
| Build | Gradle 9.6.1, Android Gradle Plugin 9.3.1, Java 17 target |
| Android | Compile SDK 35, target SDK 35, minimum SDK 26 |
| Application ID | `com.saldoclaro.finance` |

## Quick start

### Prerequisites

- Android Studio with Android SDK Platform 35 installed
- JDK 17
- An emulator or connected Android device running API 26 or later

### Build and install

```bash
git clone https://github.com/juankasobe/finanzasApp.git
cd finanzasApp
bash gradlew assembleDebug
bash gradlew installDebug
```

Use `bash gradlew ...` on Unix and WSL because the wrapper's executable bit is not assumed. On Windows Command Prompt or PowerShell, use `gradlew.bat` instead.

You can also open the repository in Android Studio and run the `app` configuration on an emulator or connected device.

## Testing

Run the JVM suite without a device:

```bash
bash gradlew testDebugUnitTest
```

The 26 JVM tests cover domain calculations, fixed Spanish currency and date presentation, and transaction, dashboard, and budget ViewModel behavior.

Run the connected Android suite with an API 26+ emulator or device available:

```bash
bash gradlew connectedDebugAndroidTest
```

The 20 connected tests comprise:

- 12 Compose tests for Spanish UI behavior, accessibility semantics, dashboard states, and budget management.
- 8 Room tests for categories, transaction history, budget mutations, and persistence invariants.

## Architecture

The app uses a layered, single-module structure with explicit repository boundaries:

```text
Compose screens and navigation
            |
        ViewModels
            |
Domain models, use cases, and repository contracts
            |
Room repository implementations
            |
     Room database and DAOs
```

`AppContainer` creates the Room database, repositories, current-month source, and screen ViewModels. UI state is exposed through `StateFlow`, while Room `Flow` queries keep current-month transactions and budgets synchronized with the dashboard.

## Project map

| Path | Responsibility |
|---|---|
| [`app/src/main/java/com/saldoclaro/finance/navigation`](app/src/main/java/com/saldoclaro/finance/navigation) | Top-level Compose navigation and screen wiring |
| [`app/src/main/java/com/saldoclaro/finance/feature`](app/src/main/java/com/saldoclaro/finance/feature) | Dashboard, transactions, categories, and budgets |
| [`app/src/main/java/com/saldoclaro/finance/domain`](app/src/main/java/com/saldoclaro/finance/domain) | Finance models, use cases, and repository contracts |
| [`app/src/main/java/com/saldoclaro/finance/data`](app/src/main/java/com/saldoclaro/finance/data) | Room entities, DAOs, database, and repository implementations |
| [`app/src/main/java/com/saldoclaro/finance/core`](app/src/main/java/com/saldoclaro/finance/core) | Design system, presentation rules, and current-month time source |
| [`app/src/test`](app/src/test) | JVM unit and ViewModel tests |
| [`app/src/androidTest`](app/src/androidTest) | Compose and Room connected tests |
| [`app/schemas`](app/schemas) | Exported Room database schema |

## Data and privacy

Categories, transactions, and monthly budgets are stored in the local Room database `saldo-claro.db`. Monetary values are persisted as integer cents. The application does not request the Android `INTERNET` permission, and Android application backup is disabled with `android:allowBackup="false"`.
