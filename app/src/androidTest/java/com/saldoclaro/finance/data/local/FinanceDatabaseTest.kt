package com.saldoclaro.finance.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saldoclaro.finance.data.repository.RoomFinanceRepositories
import java.time.LocalDate
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

    @Before
    fun createDatabase() {
        database = FinanceDatabase.inMemory(ApplicationProvider.getApplicationContext())
        repositories = RoomFinanceRepositories(database)
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
}
