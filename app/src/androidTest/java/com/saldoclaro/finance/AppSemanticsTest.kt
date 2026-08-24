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
        composeTestRule.onNodeWithContentDescription("Income \$0.00").assertExists()
        composeTestRule.onNodeWithContentDescription("Expenses \$0.00").assertExists()
        composeTestRule.onNodeWithContentDescription("Total balance \$0.00").assertExists()
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

        composeTestRule.onNodeWithContentDescription("Navigate to Dashboard").performClick()
        composeTestRule.onNodeWithContentDescription("Total balance \$0.00").assertExists()

        composeTestRule.onNodeWithContentDescription("Navigate to Transactions").performClick()
        composeTestRule.onNodeWithText("No transactions yet").assertExists()

        composeTestRule.onNodeWithContentDescription("Navigate to Categories").performClick()
        composeTestRule.onNodeWithText("Category name").assertExists()

        composeTestRule.onNodeWithContentDescription("Navigate to Budgets").performClick()
        composeTestRule.onNodeWithText("Save limit").assertExists()
    }
}
