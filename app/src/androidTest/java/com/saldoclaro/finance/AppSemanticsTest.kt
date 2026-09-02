package com.saldoclaro.finance

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSemanticsTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardEmptyStateHasTextualMeaningIndependentOfPalette() {
        composeTestRule.onNodeWithContentDescription("Ingresos 0,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithContentDescription("Gastos 0,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithContentDescription("Saldo total 0,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithText("No hay transacciones este mes").assertExists()
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

        composeTestRule.onNodeWithContentDescription("Ir a Panel").performClick()
        composeTestRule.onNodeWithContentDescription("Saldo total 0,00\u00a0US\$").assertExists()

        composeTestRule.onNodeWithContentDescription("Ir a Movimientos").performClick()
        composeTestRule.onNodeWithText("Aún no hay transacciones").assertExists()

        composeTestRule.onNodeWithContentDescription("Ir a Categorías").performClick()
        composeTestRule.onNodeWithText("Nombre de la categoría").assertExists()

        composeTestRule.onNodeWithContentDescription("Ir a Presupuestos").performClick()
        composeTestRule.onNodeWithText("Guardar límite").assertExists()
    }

    @Test
    fun applicationKeepsSpanishCopyWhenDeviceUsesUnsupportedLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            composeTestRule.onNodeWithContentDescription("Saldo total 0,00\u00a0US\$").assertExists()
            composeTestRule.onNodeWithText("No hay transacciones este mes").assertExists()
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
