package com.saldoclaro.finance.feature.dashboard

import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.core.time.CurrentMonthSource
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.BudgetTarget
import com.saldoclaro.finance.domain.repository.DeleteEvidence
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardViewModelTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:30:00Z"), zone)
    private val localCurrentMonth = YearMonth.of(2026, 3)

    @Test
    fun `dashboard stays loading until both current month sources emit`() = runBlocking {
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val viewModel = viewModel(transactions, budgets)

        assertTrue(viewModel.state.value is DashboardUiState.Loading)

        transactions.publish(emptyList())
        assertTrue(viewModel.state.value is DashboardUiState.Loading)

        budgets.publish(emptyList())
        val state = content(viewModel.state.value)
        assertEquals(MonthTotals(0L, 0L), state.totals)
        assertEquals(emptyList<Transaction>(), state.recentActivity)
        assertEquals(DashboardBudgetOverview.NoBudgets, state.budgetOverview)
    }

    @Test
    fun `fixed clock and zone show ordered local month totals and budget progress`() = runBlocking {
        val income = transaction("income", TransactionType.INCOME, 10_000L, LocalDate.of(2026, 3, 1))
        val firstExpense = transaction("first-expense", TransactionType.EXPENSE, 2_500L, LocalDate.of(2026, 3, 15))
        val lastExpense = transaction("last-expense", TransactionType.EXPENSE, 500L, LocalDate.of(2026, 3, 31))
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val viewModel = viewModel(transactions, budgets)

        transactions.publish(
            listOf(
                transaction("outside-before", TransactionType.EXPENSE, 90_000L, LocalDate.of(2026, 2, 28)),
                income,
                firstExpense,
                lastExpense,
                transaction("outside-after", TransactionType.INCOME, 90_000L, LocalDate.of(2026, 4, 1)),
            ),
        )
        assertTrue(viewModel.state.value is DashboardUiState.Loading)

        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 3_000L)))
        val state = content(viewModel.state.value)

        assertEquals(localCurrentMonth, transactions.observedMonths.single())
        assertEquals(localCurrentMonth, budgets.observedMonths.single())
        assertEquals(MonthTotals(10_000L, 3_000L), state.totals)
        assertEquals(7_000L, state.totals.balanceCents)
        assertEquals(listOf(income, firstExpense, lastExpense), state.recentActivity)
        val progress = (state.budgetOverview as DashboardBudgetOverview.Progress).items.single()
        assertEquals("groceries", progress.categoryId)
        assertEquals(BudgetState.AT_LIMIT, progress.state)
        assertEquals(3_000L, progress.limitCents)
        assertEquals(3_000L, progress.spentCents)
        assertEquals(0L, progress.remainingCents)
    }

    @Test
    fun `no budgets retains current month totals and activity`() = runBlocking {
        val activity = transaction("expense", TransactionType.EXPENSE, 2_500L, LocalDate.of(2026, 3, 20))
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val viewModel = viewModel(transactions, budgets)

        transactions.publish(listOf(activity))
        budgets.publish(emptyList())

        val state = content(viewModel.state.value)
        assertEquals(MonthTotals(0L, 2_500L), state.totals)
        assertEquals(listOf(activity), state.recentActivity)
        val progress = (state.budgetOverview as DashboardBudgetOverview.Progress).items.single()
        assertEquals("groceries", progress.categoryId)
        assertEquals(BudgetState.NO_BUDGET, progress.state)
        assertEquals(null, progress.limitCents)
        assertEquals(2_500L, progress.spentCents)
    }

    @Test
    fun `month refresh switches dashboard projection to the new local month`() = runBlocking {
        val next = localCurrentMonth.plusMonths(1)
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val source = TestMonthSource(localCurrentMonth)
        val viewModel = viewModel(transactions, budgets, source)

        source.advance(next)
        source.refresh()
        val expense = transaction("next-expense", TransactionType.EXPENSE, 1_500L, next.atDay(2))
        transactions.publish(listOf(expense))
        budgets.publish(listOf(Budget("groceries", next, 2_000L)))

        val progress = (content(viewModel.state.value).budgetOverview as DashboardBudgetOverview.Progress).items.single()
        assertEquals(next, transactions.observedMonths.last())
        assertEquals(next, budgets.observedMonths.last())
        assertEquals(BudgetState.UNDER, progress.state)
        assertEquals(1_500L, progress.spentCents)
    }

    @Test
    fun `either source failure clears dashboard data and retry waits for fresh pair`() = runBlocking {
        val stale = transaction("stale", TransactionType.INCOME, 10_000L, LocalDate.of(2026, 3, 2))
        val fresh = transaction("fresh", TransactionType.EXPENSE, 2_500L, LocalDate.of(2026, 3, 3))
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val viewModel = viewModel(transactions, budgets)
        transactions.publish(listOf(stale))
        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 5_000L)))
        assertEquals(MonthTotals(10_000L, 0L), content(viewModel.state.value).totals)

        budgets.failRead(IllegalStateException("storage unavailable"))
        val failed = error(viewModel.state.value)
        assertTrue(failed.canRetry)

        viewModel.retry()
        assertTrue(viewModel.state.value is DashboardUiState.Loading)
        transactions.publish(listOf(fresh))
        assertTrue(viewModel.state.value is DashboardUiState.Loading)
        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 3_000L)))

        val recovered = content(viewModel.state.value)
        assertEquals(MonthTotals(0L, 2_500L), recovered.totals)
        assertEquals(-2_500L, recovered.totals.balanceCents)
        assertEquals(listOf(fresh), recovered.recentActivity)
    }

    private fun viewModel(
        transactions: TransactionRepository,
        budgets: BudgetRepository,
        monthSource: CurrentMonthSource = TestMonthSource(localCurrentMonth),
    ) = DashboardViewModel(
        transactionRepository = transactions,
        budgetRepository = budgets,
        clock = clock,
        zone = zone,
        monthSource = monthSource,
        dispatcher = Dispatchers.Unconfined,
    )

    private fun transaction(
        id: String,
        type: TransactionType,
        amountCents: Long,
        localDate: LocalDate,
    ) = Transaction(id, type, amountCents, "groceries", localDate)

    private fun content(state: DashboardUiState): DashboardUiState.Content {
        assertTrue("Expected dashboard content but was $state", state is DashboardUiState.Content)
        return state as DashboardUiState.Content
    }

    private fun error(state: DashboardUiState): DashboardUiState.Error {
        assertTrue("Expected recoverable dashboard error but was $state", state is DashboardUiState.Error)
        return state as DashboardUiState.Error
    }

    private class TestMonthSource(initial: YearMonth) : CurrentMonthSource {
        override val month = MutableStateFlow(initial)
        private var pending = initial
        fun advance(next: YearMonth) { pending = next }
        override fun refresh() { month.value = pending }
        override fun setForeground(active: Boolean) = Unit
    }

    private class ControlledTransactionRepository : TransactionRepository {
        private val events = MutableSharedFlow<ReadEvent>(extraBufferCapacity = 16)
        val observedMonths = mutableListOf<YearMonth>()

        override fun observeMonth(month: YearMonth): Flow<List<Transaction>> {
            observedMonths += month
            return events.map { event ->
                when (event) {
                    is ReadEvent.Data -> event.transactions.filter { YearMonth.from(it.localDate) == month }
                    is ReadEvent.Failure -> throw event.error
                }
            }
        }

        fun publish(transactions: List<Transaction>) = events.tryEmit(ReadEvent.Data(transactions))

        fun failRead(error: Throwable) = events.tryEmit(ReadEvent.Failure(error))

        override suspend fun save(draft: TransactionDraft): Result<Transaction> =
            error("Dashboard does not save transactions")

        override suspend fun delete(id: String): Result<Unit> =
            error("Dashboard does not delete transactions")
    }

    private class ControlledBudgetRepository : BudgetRepository {
        private val events = MutableSharedFlow<BudgetReadEvent>(extraBufferCapacity = 16)
        val observedMonths = mutableListOf<YearMonth>()

        override fun observeMonth(month: YearMonth): Flow<List<Budget>> {
            observedMonths += month
            return events.map { event ->
                when (event) {
                    is BudgetReadEvent.Data -> event.budgets.filter { it.month == month }
                    is BudgetReadEvent.Failure -> throw event.error
                }
            }
        }

        fun publish(budgets: List<Budget>) = events.tryEmit(BudgetReadEvent.Data(budgets))

        fun failRead(error: Throwable) = events.tryEmit(BudgetReadEvent.Failure(error))

        override suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit> =
            Result.success(Unit)

        override suspend fun rollover(from: YearMonth, to: YearMonth): Result<Unit> = Result.success(Unit)

        override suspend fun editAmount(target: BudgetTarget, newLimitCents: Long): Result<Unit> =
            error("Dashboard does not edit budgets")

        override suspend fun delete(target: BudgetTarget): Result<DeleteEvidence> =
            error("Dashboard does not delete budgets")
    }

    private sealed interface ReadEvent {
        data class Data(val transactions: List<Transaction>) : ReadEvent
        data class Failure(val error: Throwable) : ReadEvent
    }

    private sealed interface BudgetReadEvent {
        data class Data(val budgets: List<Budget>) : BudgetReadEvent
        data class Failure(val error: Throwable) : BudgetReadEvent
    }
}
