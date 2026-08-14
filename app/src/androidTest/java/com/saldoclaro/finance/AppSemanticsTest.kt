package com.saldoclaro.finance

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSemanticsTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardEmptyStateHasTextualMeaningIndependentOfPalette() {
        composeTestRule.onNodeWithText("Income: 0 cents").assertExists()
        composeTestRule.onNodeWithText("Expenses: 0 cents").assertExists()
        composeTestRule.onNodeWithText("Balance: 0 cents").assertExists()
        composeTestRule.onNodeWithText("No transactions this month").assertExists()
    }

    @Test
    fun appOffersOnlyApprovedOfflineDestinationsThroughAccessibleLabels() {
        listOf(
            "Recurring transactions",
            "Reminders",
            "Backup",
            "Restore",
            "Sign in",
            "Sync",
            "Share",
        ).forEach { deferredAction ->
            composeTestRule.onNodeWithText(deferredAction).assertDoesNotExist()
        }

        listOf(
            "Dashboard" to "Income: 0 cents",
            "Transactions" to "No transactions yet",
            "Categories" to "Category name",
            "Budgets" to "Monthly limit",
        ).forEach { (destination, screenText) ->
            composeTestRule
                .onNodeWithContentDescription("Navigate to $destination")
                .performClick()

            composeTestRule.onNodeWithText(screenText).assertExists()
        }
    }
}
