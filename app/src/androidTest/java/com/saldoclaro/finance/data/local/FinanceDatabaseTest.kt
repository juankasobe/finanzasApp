package com.saldoclaro.finance.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saldoclaro.finance.data.repository.RoomBudgetRepository
import com.saldoclaro.finance.data.repository.RoomFinanceRepositories
import com.saldoclaro.finance.domain.repository.BudgetMutationError
import com.saldoclaro.finance.domain.repository.BudgetMutationException
import com.saldoclaro.finance.domain.repository.BudgetTarget
import com.saldoclaro.finance.domain.repository.DeleteEvidence
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanceDatabaseTest {
    private lateinit var database: FinanceDatabase
    private lateinit var repositories: RoomFinanceRepositories
    private lateinit var budgetRepository: RoomBudgetRepository
    private val targetMonth = YearMonth.of(2026, 8)

    @Before
    fun createDatabase() {
        database = FinanceDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repositories = RoomFinanceRepositories(database)
        budgetRepository = RoomBudgetRepository(database)
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun builtInCategoriesAreSeededWithStableIds() = runBlocking {
        val categories = database.categoryDao().observeAll().first()

        val groceries = categories.single { it.id == "builtin-groceries" }
        val salary = categories.single { it.id == "builtin-salary" }
        assertTrue(groceries.isBuiltIn)
        assertEquals("Supermercado", groceries.name)
        assertEquals("groceries", groceries.normalizedName)
        assertTrue(salary.isBuiltIn)
        assertEquals("Salario", salary.name)
        assertEquals("salary", salary.normalizedName)
    }

    @Test
    fun customNamesAreNormalizedAndMustBeUnique() = runBlocking {
        assertTrue(repositories.createCategory("Coffee").isSuccess)
        val duplicate = repositories.createCategory(" coffee ")

        assertTrue(duplicate.isFailure)
        assertEquals("coffee", database.categoryDao().observeAll().first().single { it.name == "Coffee" }.normalizedName)
    }

    @Test
    fun archivedCustomCategoryKeepsHistoricalTransactionLabel() = runBlocking {
        val category = repositories.createCategory("Travel").getOrThrow()
        database.transactionDao().insert(
            TransactionEntity(
                id = "trip",
                type = "EXPENSE",
                amountCents = 4_200,
                categoryId = category.id,
                localDate = LocalDate.of(2026, 8, 1),
            ),
        )

        repositories.archiveCustomCategory(category.id).getOrThrow()

        assertEquals("Travel", database.transactionDao().observeAll().first().single().categoryName)
    }

    @Test
    fun archivedCustomCategoryCannotBeAssignedToNewTransaction() = runBlocking {
        val category = repositories.createCategory("Travel").getOrThrow()
        repositories.archiveCustomCategory(category.id).getOrThrow()

        val result = repositories.saveTransaction(
            id = "new-trip",
            categoryId = category.id,
            amountCents = 4_200,
            localDate = LocalDate.of(2026, 8, 2),
        )

        assertTrue(result.isFailure)
        assertFalse(database.transactionDao().observeAll().first().any { it.id == "new-trip" })
    }

    @Test
    fun editingBudgetChangesOnlyTheOpenedCategoryMonthAndPreservesTransactions() = runBlocking {
        insertBudget("builtin-groceries", targetMonth, 50_000)
        insertBudget("builtin-groceries", targetMonth.plusMonths(1), 60_000)
        insertBudget("builtin-salary", targetMonth, 70_000)
        database.transactionDao().insert(
            TransactionEntity("purchase", "EXPENSE", 4_200, "builtin-groceries", LocalDate.of(2026, 8, 12)),
        )

        val result = budgetRepository.editAmount(
            BudgetTarget("builtin-groceries", targetMonth, openedLimitCents = 50_000),
            newLimitCents = 75_000,
        )

        assertTrue(result.isSuccess)
        assertEquals(
            75_000L,
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .single { it.categoryId == "builtin-groceries" }.limitCents,
        )
        assertEquals(
            60_000L,
            database.budgetDao().observeMonth(targetMonth.plusMonths(1).toString()).first()
                .single { it.categoryId == "builtin-groceries" }.limitCents,
        )
        assertEquals(
            70_000L,
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .single { it.categoryId == "builtin-salary" }.limitCents,
        )
        assertEquals("purchase", database.transactionDao().observeAll().first().single().id)
    }

    @Test
    fun deletingBudgetReturnsAffectedRowAndLeavesOtherMonthsCategoriesAndTransactions() = runBlocking {
        insertBudget("builtin-groceries", targetMonth, 50_000)
        insertBudget("builtin-groceries", targetMonth.plusMonths(1), 60_000)
        insertBudget("builtin-salary", targetMonth, 70_000)
        database.transactionDao().insert(
            TransactionEntity("purchase", "EXPENSE", 4_200, "builtin-groceries", LocalDate.of(2026, 8, 12)),
        )

        val result = budgetRepository.delete(
            BudgetTarget("builtin-groceries", targetMonth, openedLimitCents = 50_000),
        )

        assertEquals(DeleteEvidence(affectedRows = 1), result.getOrThrow())
        assertTrue(
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .none { it.categoryId == "builtin-groceries" },
        )
        assertEquals(
            60_000L,
            database.budgetDao().observeMonth(targetMonth.plusMonths(1).toString()).first()
                .single { it.categoryId == "builtin-groceries" }.limitCents,
        )
        assertEquals(
            70_000L,
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .single { it.categoryId == "builtin-salary" }.limitCents,
        )
        assertEquals("purchase", database.transactionDao().observeAll().first().single().id)
    }

    @Test
    fun staleAndMissingTargetsReturnTypedErrorsWithoutChangingExistingLimits() = runBlocking {
        insertBudget("builtin-groceries", targetMonth, 50_000)

        val stale = budgetRepository.editAmount(
            BudgetTarget("builtin-groceries", targetMonth, openedLimitCents = 40_000),
            newLimitCents = 75_000,
        )
        val staleDelete = budgetRepository.delete(
            BudgetTarget("builtin-groceries", targetMonth, openedLimitCents = 40_000),
        )
        val missing = budgetRepository.delete(
            BudgetTarget("builtin-salary", targetMonth, openedLimitCents = 70_000),
        )

        assertEquals(BudgetMutationError.TargetStale, mutationError(stale))
        assertEquals(BudgetMutationError.TargetStale, mutationError(staleDelete))
        assertEquals(BudgetMutationError.TargetMissing, mutationError(missing))
        assertEquals(
            50_000L,
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .single { it.categoryId == "builtin-groceries" }.limitCents,
        )
    }

    @Test
    fun invalidAndArchivedEditsAreRejectedButArchivedDeletionSucceeds() = runBlocking {
        insertBudget("builtin-groceries", targetMonth, 50_000)
        val archivedCategory = repositories.createCategory("Travel").getOrThrow()
        insertBudget(archivedCategory.id, targetMonth, 25_000)
        repositories.archiveCustomCategory(archivedCategory.id).getOrThrow()

        val invalid = budgetRepository.editAmount(
            BudgetTarget("builtin-groceries", targetMonth, openedLimitCents = 50_000),
            newLimitCents = 0,
        )
        val archived = budgetRepository.editAmount(
            BudgetTarget(archivedCategory.id, targetMonth, openedLimitCents = 25_000),
            newLimitCents = 30_000,
        )
        val deleted = budgetRepository.delete(
            BudgetTarget(archivedCategory.id, targetMonth, openedLimitCents = 25_000),
        )

        assertEquals(BudgetMutationError.InvalidLimit, mutationError(invalid))
        assertEquals(BudgetMutationError.ArchivedCategory, mutationError(archived))
        assertEquals(DeleteEvidence(affectedRows = 1), deleted.getOrThrow())
        assertEquals(
            50_000L,
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .single { it.categoryId == "builtin-groceries" }.limitCents,
        )
        assertTrue(
            database.budgetDao().observeMonth(targetMonth.toString()).first()
                .none { it.categoryId == archivedCategory.id },
        )
    }

    private suspend fun insertBudget(categoryId: String, month: YearMonth, limitCents: Long) {
        database.budgetDao().upsert(BudgetEntity(categoryId, month.toString(), limitCents))
    }

    private fun mutationError(result: Result<*>): BudgetMutationError {
        val exception = result.exceptionOrNull()
        assertTrue(exception is BudgetMutationException)
        return (exception as BudgetMutationException).reason
    }
}
