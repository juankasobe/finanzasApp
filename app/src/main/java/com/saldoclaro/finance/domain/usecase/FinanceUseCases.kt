package com.saldoclaro.finance.domain.usecase

import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionType
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class BudgetState { NO_BUDGET, UNDER, AT_LIMIT, OVER }

data class BudgetProgress(val state: BudgetState, val remainingCents: Long?)

fun requirePositiveCents(cents: Long): Long = cents.also {
    require(it > 0) { "Cents must be positive" }
}

fun currentMonthBounds(clock: Clock, zone: ZoneId): ClosedRange<LocalDate> =
    currentMonth(clock, zone).let { it.atDay(1)..it.atEndOfMonth() }

fun currentMonth(clock: Clock, zone: ZoneId): YearMonth = YearMonth.from(clock.instant().atZone(zone))

fun calculateMonthTotals(
    transactions: List<Transaction>,
    monthBounds: ClosedRange<LocalDate>,
): MonthTotals {
    val currentMonth = transactions.filter { it.localDate in monthBounds }
    return MonthTotals(
        incomeCents = currentMonth.totalFor(TransactionType.INCOME),
        expenseCents = currentMonth.totalFor(TransactionType.EXPENSE),
    )
}

private fun List<Transaction>.totalFor(type: TransactionType): Long =
    filter { it.type == type }.sumOf { it.amountCents }

fun calculateBudgetProgress(limitCents: Long?, expenseCents: Long): BudgetProgress {
    if (limitCents == null) return BudgetProgress(BudgetState.NO_BUDGET, null)
    val remaining = limitCents - expenseCents
    val state = when {
        remaining > 0 -> BudgetState.UNDER
        remaining == 0L -> BudgetState.AT_LIMIT
        else -> BudgetState.OVER
    }
    return BudgetProgress(state, remaining)
}
