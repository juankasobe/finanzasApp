package com.saldoclaro.finance.domain.repository

import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeMonth(month: YearMonth): Flow<List<Transaction>>
    suspend fun save(draft: TransactionDraft): Result<Transaction>
    suspend fun delete(id: String): Result<Unit>
}

interface BudgetRepository {
    fun observeMonth(month: YearMonth): Flow<List<Budget>>
    suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit>
}
