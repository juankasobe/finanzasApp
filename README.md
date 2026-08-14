# Saldo Claro

Saldo Claro is a focused, offline Android finance tracker for recording USD income and expenses and monthly category budgets. It stores money as integer cents and does not request internet access.

## Quick path

1. Open `C:\dev\finanzasApp` in Android Studio, or use a terminal there.
2. Ensure Android SDK Platform 35 is installed and set `ANDROID_HOME` to the SDK if Android Studio has not configured it.
3. Run `gradlew.bat testDebugUnitTest` and then `gradlew.bat installDebug` with a connected device or emulator.

## MVP behavior

| Area | Included behavior |
|---|---|
| Dashboard | Current-month income, expenses, net balance, and recent transactions |
| Transactions | Add income or expense transactions using built-in categories; delete transactions |
| Budgets | Monthly per-expense-category limits with on-track, near-limit, and over-limit states |
| Privacy | Local-only storage, no `INTERNET` permission, and an explicit device-loss warning |
