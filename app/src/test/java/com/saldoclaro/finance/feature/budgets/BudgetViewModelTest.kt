package com.saldoclaro.finance.feature.budgets

import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.domain.usecase.BudgetState
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetViewModelTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:30:00Z"), zone)
    private val localCurrentMonth = YearMonth.of(2026, 3)

    @Test
    fun `fixed clock exposes expense-only under at limit over and no-budget progress`() = runBlocking {
        val transactions = FakeTransactionRepository(
            listOf(
                transaction("under-expense", TransactionType.EXPENSE, 3_000L, "under"),
                transaction("under-income", TransactionType.INCOME, 90_000L, "under"),
                transaction("at-expense", TransactionType.EXPENSE, 4_000L, "at-limit"),
                transaction("over-expense", TransactionType.EXPENSE, 6_001L, "over"),
                transaction("none-expense", TransactionType.EXPENSE, 1_500L, "no-budget"),
                transaction("other-month", TransactionType.EXPENSE, 90_000L, "under", LocalDate.of(2026, 4, 1)),
            ),
        )
        val budgets = FakeBudgetRepository(
            listOf(budget("under", 10_000L), budget("at-limit", 4_000L), budget("over", 6_000L)),
        )

        val progress = content(viewModel(transactions, budgets).state.value).progress.associateBy { it.categoryId }

        assertEquals(localCurrentMonth, transactions.observedMonths.single())
        assertEquals(localCurrentMonth, budgets.observedMonths.single())
        assertEquals(setOf("under", "at-limit", "over", "no-budget"), progress.keys)
        assertProgress(progress.getValue("under"), BudgetState.UNDER, 10_000L, 3_000L, 7_000L)
        assertProgress(progress.getValue("at-limit"), BudgetState.AT_LIMIT, 4_000L, 4_000L, 0L)
        assertProgress(progress.getValue("over"), BudgetState.OVER, 6_000L, 6_001L, -1L)
        assertProgress(progress.getValue("no-budget"), BudgetState.NO_BUDGET, null, 1_500L, null)
    }

    @Test
    fun `zero negative fractional and blank limits are rejected without saving`() = runBlocking {
        val budgets = FakeBudgetRepository(emptyList())
        val viewModel = viewModel(FakeTransactionRepository(emptyList()), budgets)

        viewModel.saveLimit("groceries", "0.00")
        assertEquals(setOf(BudgetField.LIMIT), validation(viewModel.state.value).invalidFields)
        viewModel.saveLimit("groceries", "-1.00")
        assertEquals(setOf(BudgetField.LIMIT), validation(viewModel.state.value).invalidFields)
        viewModel.saveLimit("groceries", "12.345")
        assertEquals(setOf(BudgetField.LIMIT), validation(viewModel.state.value).invalidFields)
        viewModel.saveLimit("groceries", " ")
        assertEquals(setOf(BudgetField.LIMIT), validation(viewModel.state.value).invalidFields)
        assertEquals(0, budgets.saveCalls)
    }

    @Test
    fun `budget read failure clears stale progress and retry loads fresh values`() = runBlocking {
        val transactions = FakeTransactionRepository(listOf(transaction("stale", TransactionType.EXPENSE, 2_000L, "groceries")))
        val budgets = FakeBudgetRepository(listOf(budget("groceries", 5_000L)))
        val viewModel = viewModel(transactions, budgets)

        assertProgress(content(viewModel.state.value).progress.single(), BudgetState.UNDER, 5_000L, 2_000L, 3_000L)

        budgets.failRead(IllegalStateException("storage unavailable"))

        val failed = error(viewModel.state.value)
        assertTrue(failed.canRetry)
        assertEquals(emptyList<BudgetProgressItem>(), failed.progress)

        transactions.publish(listOf(transaction("fresh", TransactionType.EXPENSE, 4_000L, "groceries")))
        budgets.publish(listOf(budget("groceries", 3_000L)))
        viewModel.retry()

        assertProgress(content(viewModel.state.value).progress.single(), BudgetState.OVER, 3_000L, 4_000L, -1_000L)
    }

    private fun viewModel(transactions: TransactionRepository, budgets: BudgetRepository) = BudgetViewModel(
        transactionRepository = transactions,
        budgetRepository = budgets,
        clock = clock,
        zone = zone,
        dispatcher = Dispatchers.Unconfined,
    )

    private fun transaction(
        id: String,
        type: TransactionType,
        amountCents: Long,
        categoryId: String,
        localDate: LocalDate = LocalDate.of(2026, 3, 15),
    ) = Transaction(id, type, amountCents, categoryId, localDate)

    private fun budget(categoryId: String, limitCents: Long) = Budget(categoryId, localCurrentMonth, limitCents)

    private fun assertProgress(
        actual: BudgetProgressItem,
        state: BudgetState,
        limitCents: Long?,
        spentCents: Long,
        remainingCents: Long?,
    ) {
        assertEquals(state, actual.state)
        assertEquals(limitCents, actual.limitCents)
        assertEquals(spentCents, actual.spentCents)
        assertEquals(remainingCents, actual.remainingCents)
    }

    private fun content(state: BudgetUiState): BudgetUiState.Content {
        assertTrue("Expected budget content but was $state", state is BudgetUiState.Content)
        return state as BudgetUiState.Content
    }

    private fun validation(state: BudgetUiState): BudgetUiState.Validation {
        assertTrue("Expected budget validation but was $state", state is BudgetUiState.Validation)
        return state as BudgetUiState.Validation
    }

    private fun error(state: BudgetUiState): BudgetUiState.Error {
        assertTrue("Expected recoverable budget error but was $state", state is BudgetUiState.Error)
        return state as BudgetUiState.Error
    }

    private class FakeTransactionRepository(initial: List<Transaction>) : TransactionRepository {
        private val events = ReadEvents(initial)
        val observedMonths = mutableListOf<YearMonth>()

        override fun observeMonth(month: YearMonth): Flow<List<Transaction>> {
            observedMonths += month
            return events.observe { YearMonth.from(it.localDate) == month }
        }

        fun publish(transactions: List<Transaction>) = events.publish(transactions)
        override suspend fun save(draft: TransactionDraft): Result<Transaction> = error("Budget does not save transactions")
        override suspend fun delete(id: String): Result<Unit> = error("Budget does not delete transactions")
    }

    private class FakeBudgetRepository(initial: List<Budget>) : BudgetRepository {
        private val events = ReadEvents(initial)
        val observedMonths = mutableListOf<YearMonth>()
        var saveCalls = 0

        override fun observeMonth(month: YearMonth): Flow<List<Budget>> {
            observedMonths += month
            return events.observe { it.month == month }
        }

        override suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit> {
            saveCalls += 1
            return Result.success(Unit)
        }

        fun publish(budgets: List<Budget>) = events.publish(budgets)
        fun failRead(error: Throwable) = events.fail(error)
    }

    private class ReadEvents<T>(initial: List<T>) {
        private val events = MutableSharedFlow<ReadEvent<List<T>>>(replay = 1)

        init {
            publish(initial)
        }

        fun observe(matches: (T) -> Boolean): Flow<List<T>> = events.map { event ->
            when (event) {
                is ReadEvent.Data -> event.values.filter(matches)
                is ReadEvent.Failure -> throw event.error
            }
        }

        fun publish(values: List<T>) = publish(ReadEvent.Data(values))
        fun fail(error: Throwable) = publish(ReadEvent.Failure(error))

        private fun publish(event: ReadEvent<List<T>>) {
            check(events.tryEmit(event))
        }
    }

    private sealed interface ReadEvent<out T> {
        data class Data<T>(val values: T) : ReadEvent<T>
        data class Failure(val error: Throwable) : ReadEvent<Nothing>
    }
}
