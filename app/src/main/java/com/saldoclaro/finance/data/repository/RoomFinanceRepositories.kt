package com.saldoclaro.finance.data.repository

import androidx.room.withTransaction
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.data.local.BudgetEntity
import com.saldoclaro.finance.data.local.FinanceDatabase
import com.saldoclaro.finance.data.local.TransactionEntity
import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.BudgetMutationError
import com.saldoclaro.finance.domain.repository.BudgetMutationException
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.BudgetTarget
import com.saldoclaro.finance.domain.repository.DeleteEvidence
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

    override suspend fun rollover(from: YearMonth, to: YearMonth): Result<Unit> = databaseResult {
        database.withTransaction {
            val current = database.budgetDao().findAll(to.toString()).mapTo(mutableSetOf()) { it.categoryId }
            database.budgetDao().findAll(from.toString()).forEach { prior ->
                if (database.categoryDao().find(prior.categoryId)?.isArchived == false && current.add(prior.categoryId)) {
                    database.budgetDao().upsert(prior.copy(monthKey = to.toString()))
                }
            }
        }
    }

    override suspend fun editAmount(target: BudgetTarget, newLimitCents: Long): Result<Unit> = databaseResult {
        if (newLimitCents <= 0) throw BudgetMutationException(BudgetMutationError.InvalidLimit)
        database.withTransaction {
            requireCurrentTarget(target)
            requireActiveCategory(target.categoryId)
            val affectedRows = database.budgetDao().updateLimit(
                target.categoryId,
                target.month.toString(),
                target.openedLimitCents,
                newLimitCents,
            )
            if (affectedRows != 1) {
                throw BudgetMutationException(BudgetMutationError.UnexpectedAffectedRows(affectedRows))
            }
        }
    }

    override suspend fun delete(target: BudgetTarget): Result<DeleteEvidence> = databaseResult {
        database.withTransaction {
            requireCurrentTarget(target)
            val affectedRows = database.budgetDao().delete(
                target.categoryId,
                target.month.toString(),
                target.openedLimitCents,
            )
            if (affectedRows != 1) {
                throw BudgetMutationException(BudgetMutationError.UnexpectedAffectedRows(affectedRows))
            }
            DeleteEvidence(affectedRows)
        }
    }

    private suspend fun requireCurrentTarget(target: BudgetTarget) {
        val current = database.budgetDao().find(target.categoryId, target.month.toString())
            ?: throw BudgetMutationException(BudgetMutationError.TargetMissing)
        if (current.limitCents != target.openedLimitCents) {
            throw BudgetMutationException(BudgetMutationError.TargetStale)
        }
    }

    private suspend fun requireActiveCategory(categoryId: String) {
        val category = database.categoryDao().find(categoryId)
        when {
            category == null -> throw BudgetMutationException(BudgetMutationError.CategoryNotFound)
            category.isArchived -> throw BudgetMutationException(BudgetMutationError.ArchivedCategory)
        }
    }
}

internal fun normalizeCategoryName(name: String): String = name.trim().lowercase().also {
    require(it.isNotBlank()) { "Category name cannot be blank" }
}

private suspend fun <T> databaseResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (error: BudgetMutationException) {
    Result.failure(error)
} catch (error: IllegalArgumentException) {
    Result.failure(error)
} catch (error: Exception) {
    Result.failure(IllegalStateException("Database operation failed", error))
}

private fun TransactionEntity.toTransaction() = Transaction(id, TransactionType.valueOf(type), amountCents, categoryId, localDate)
private fun BudgetEntity.toBudget() = Budget(categoryId, YearMonth.parse(monthKey), limitCents)
private fun String.toCents(): Long = trim().toBigDecimal().movePointRight(2).longValueExact()
