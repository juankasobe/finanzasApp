package com.saldoclaro.finance.domain.repository

import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

data class BudgetTarget(val categoryId: String, val month: YearMonth, val openedLimitCents: Long)
data class DeleteEvidence(val affectedRows: Int)

sealed interface BudgetMutationError {
    data object InvalidLimit : BudgetMutationError
    data object CategoryNotFound : BudgetMutationError
    data object ArchivedCategory : BudgetMutationError
    data object TargetMissing : BudgetMutationError
    data object TargetStale : BudgetMutationError
    data class UnexpectedAffectedRows(val affectedRows: Int) : BudgetMutationError
}

class BudgetMutationException(val reason: BudgetMutationError) : IllegalStateException(reason.toString())

interface TransactionRepository {
    fun observeMonth(month: YearMonth): Flow<List<Transaction>>
    suspend fun save(draft: TransactionDraft): Result<Transaction>
    suspend fun delete(id: String): Result<Unit>
}

interface BudgetRepository {
    fun observeMonth(month: YearMonth): Flow<List<Budget>>
    suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit>
    suspend fun rollover(from: YearMonth, to: YearMonth): Result<Unit> = Result.success(Unit)
    suspend fun editAmount(target: BudgetTarget, newLimitCents: Long): Result<Unit>
    suspend fun delete(target: BudgetTarget): Result<DeleteEvidence>
}
