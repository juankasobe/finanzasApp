package com.saldoclaro.finance.domain.model

import java.time.LocalDate
import java.time.YearMonth

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: String,
    val localDate: LocalDate,
)

data class TransactionDraft(
    val type: TransactionType?,
    val amount: String,
    val categoryId: String?,
    val localDate: LocalDate?,
)

data class MonthTotals(
    val incomeCents: Long,
    val expenseCents: Long,
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

data class Budget(
    val categoryId: String,
    val month: YearMonth,
    val limitCents: Long,
)
