package com.saldoclaro.finance.domain.usecase

import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionType
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyDateUseCasesTest {
    private val zone = ZoneId.of("America/New_York")
    private val clock = Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), zone)

    @Test
    fun `requirePositiveCents accepts positive whole cents and rejects nonpositive values`() {
        assertEquals(125L, requirePositiveCents(125))
        assertThrows(IllegalArgumentException::class.java) { requirePositiveCents(0) }
        assertThrows(IllegalArgumentException::class.java) { requirePositiveCents(-1) }
    }

    @Test
    fun `currentMonthBounds uses the injected local clock month`() {
        assertEquals(
            LocalDate.of(2026, 3, 1)..LocalDate.of(2026, 3, 31),
            currentMonthBounds(clock, zone),
        )
    }

    @Test
    fun `calculateMonthTotals includes only transactions inside the current local month`() {
        val totals = calculateMonthTotals(
            transactions = listOf(
                transaction(TransactionType.INCOME, 50_000, LocalDate.of(2026, 3, 1)),
                transaction(TransactionType.EXPENSE, 12_345, LocalDate.of(2026, 3, 31)),
                transaction(TransactionType.EXPENSE, 99_999, LocalDate.of(2026, 2, 28)),
            ),
            monthBounds = currentMonthBounds(clock, zone),
        )

        assertEquals(50_000, totals.incomeCents)
        assertEquals(12_345, totals.expenseCents)
        assertEquals(37_655, totals.balanceCents)
    }

    @Test
    fun `calculateBudgetProgress excludes income and reports each budget state`() {
        assertEquals(BudgetState.NO_BUDGET, calculateBudgetProgress(null, 500).state)
        assertEquals(BudgetState.UNDER, calculateBudgetProgress(1_000, 999).state)
        assertEquals(BudgetState.AT_LIMIT, calculateBudgetProgress(1_000, 1_000).state)
        assertEquals(BudgetState.OVER, calculateBudgetProgress(1_000, 1_001).state)
    }

    private fun transaction(type: TransactionType, cents: Long, date: LocalDate) = Transaction(
        id = "transaction-$cents",
        type = type,
        amountCents = cents,
        categoryId = "category",
        localDate = date,
    )
}
