package com.saldoclaro.finance.data.repository

import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.data.local.BudgetEntity
import com.saldoclaro.finance.data.local.FinanceDatabase
import com.saldoclaro.finance.data.local.TransactionEntity
import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFinanceRepositories(private val database: FinanceDatabase) : TransactionRepository {
    fun observeCategories(): Flow<List<CategoryEntity>> = database.categoryDao().observeAll()

    override fun observeMonth(month: YearMonth): Flow<List<Transaction>> =
        database.transactionDao().observeMonth(month.atDay(1), month.atEndOfMonth()).map { it.map(TransactionEntity::toTransaction) }

    suspend fun createCategory(name: String): Result<CategoryEntity> = databaseResult {
        val normalized = normalizeCategoryName(name)
        CategoryEntity("custom-$normalized", name.trim(), normalized, false).also { database.categoryDao().insert(it) }
    }

    suspend fun archiveCustomCategory(id: String): Result<Unit> = databaseResult {
        check(database.categoryDao().archiveCustom(id) == 1) { "Category cannot be archived" }
    }

    suspend fun saveTransaction(id: String, categoryId: String, amountCents: Long, localDate: LocalDate): Result<Unit> = databaseResult {
        require(amountCents > 0) { "Cents must be positive" }
        check(database.categoryDao().find(categoryId)?.isArchived == false) { "Category is inactive" }
        database.transactionDao().insert(TransactionEntity(id, "EXPENSE", amountCents, categoryId, localDate))
    }

    override suspend fun save(draft: TransactionDraft): Result<Transaction> = databaseResult {
        val transaction = Transaction(UUID.randomUUID().toString(), requireNotNull(draft.type), draft.amount.toCents(), requireNotNull(draft.categoryId), requireNotNull(draft.localDate))
        require(transaction.amountCents > 0) { "Cents must be positive" }
        check(database.categoryDao().find(transaction.categoryId)?.isArchived == false) { "Category is inactive" }
        database.transactionDao().insert(
            TransactionEntity(transaction.id, transaction.type.name, transaction.amountCents, transaction.categoryId, transaction.localDate),
        )
        transaction
    }

    override suspend fun delete(id: String): Result<Unit> = databaseResult {
        check(database.transactionDao().delete(id) == 1) { "Transaction cannot be deleted" }
    }
}

class RoomBudgetRepository(private val database: FinanceDatabase) : BudgetRepository {
    override fun observeMonth(month: YearMonth): Flow<List<Budget>> =
        database.budgetDao().observeMonth(month.toString()).map { it.map(BudgetEntity::toBudget) }

    override suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit> = databaseResult {
        require(limitCents > 0) { "Cents must be positive" }
        check(database.categoryDao().find(categoryId)?.isArchived == false) { "Category is inactive" }
        database.budgetDao().upsert(BudgetEntity(categoryId, month.toString(), limitCents))
    }
}

internal fun normalizeCategoryName(name: String): String = name.trim().lowercase().also {
    require(it.isNotBlank()) { "Category name cannot be blank" }
}

private suspend fun <T> databaseResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: IllegalArgumentException) {
    Result.failure(error)
} catch (error: Exception) {
    Result.failure(IllegalStateException("Database operation failed", error))
}

private fun TransactionEntity.toTransaction() = Transaction(id, TransactionType.valueOf(type), amountCents, categoryId, localDate)
private fun BudgetEntity.toBudget() = Budget(categoryId, YearMonth.parse(monthKey), limitCents)
private fun String.toCents(): Long = trim().toBigDecimal().movePointRight(2).longValueExact()
