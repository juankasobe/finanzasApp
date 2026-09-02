package com.saldoclaro.finance.core.presentation

import com.saldoclaro.finance.R
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class UiPresentationTest {
    @Test
    fun `currency and date use fixed Spanish presentation regardless of default locale`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("12.345,67\u00a0US$", formatCents(1_234_567))
            assertEquals("-25,00\u00a0US$", formatCents(-2_500))
            assertEquals("15 mar", formatDate(LocalDate.of(2026, 3, 15)))

            Locale.setDefault(Locale.JAPAN)
            assertEquals("12.345,67\u00a0US$", formatCents(1_234_567))
            assertEquals("15 mar", formatDate(LocalDate.of(2026, 3, 15)))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `current and legacy built in ids use Spanish labels`() {
        assertEquals("Supermercado", categoryPresentationName("builtin-groceries", "Groceries"))
        assertEquals("Supermercado", categoryPresentationName("groceries", "Groceries"))
        assertEquals("Salario", categoryPresentationName("builtin-salary", "Salary"))
        assertEquals("Salario", categoryPresentationName("salary", "Salary"))
    }

    @Test
    fun `custom category names remain unchanged`() {
        val customName = "My Café / Viajes"
        val secondCustomName = "English / Mixto"

        assertEquals(customName, categoryPresentationName("custom-my-cafe-viajes", customName))
        assertEquals(secondCustomName, categoryPresentationName("custom-english-mixto", secondCustomName))
    }

    @Test
    fun `ui error keys point to safe resource messages`() {
        assertEquals(R.string.error_data_unavailable, UiErrorKey.DATA_UNAVAILABLE.resourceId)
        assertEquals(R.string.error_operation_failed, UiErrorKey.OPERATION_FAILED.resourceId)
        assertEquals(R.string.error_target_unavailable, UiErrorKey.TARGET_UNAVAILABLE.resourceId)
        assertEquals(R.string.error_invalid_amount, UiErrorKey.INVALID_AMOUNT.resourceId)
    }
}
