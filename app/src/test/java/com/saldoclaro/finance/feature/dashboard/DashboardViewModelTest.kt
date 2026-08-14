package com.saldoclaro.finance.feature.dashboard

import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.TransactionRepository
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

class DashboardViewModelTest {
    private val zone = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:30:00Z"), zone)
    private val localCurrentMonth = YearMonth.of(2026, 3)

    @Test
    fun `fixed clock and zone show only local current month exact cents`() = runBlocking {
        val income = transaction("income", TransactionType.INCOME, 50_000L, LocalDate.of(2026, 3, 1))
        val expense = transaction("expense", TransactionType.EXPENSE, 12_345L, LocalDate.of(2026, 3, 31))
        val otherMonth = transaction("future", TransactionType.INCOME, 90_000L, LocalDate.of(2026, 4, 1))
        val repository = FakeTransactionRepository(listOf(income, expense, otherMonth))

        val state = content(viewModel(repository).state.value)

        assertEquals(localCurrentMonth, repository.observedMonths.single())
        assertEquals(50_000L, state.totals.incomeCents)
        assertEquals(12_345L, state.totals.expenseCents)
        assertEquals(37_655L, state.totals.balanceCents)
        assertEquals(setOf(income, expense), state.recentActivity.toSet())
    }

    @Test
    fun `empty local current month exposes explicit zero totals`() = runBlocking {
        val otherMonth = transaction("future", TransactionType.EXPENSE, 90_000L, LocalDate.of(2026, 4, 1))

        val state = empty(viewModel(FakeTransactionRepository(listOf(otherMonth))).state.value)

        assertEquals(MonthTotals(incomeCents = 0L, expenseCents = 0L), state.totals)
        assertEquals(emptyList<Transaction>(), state.recentActivity)
    }

    @Test
    fun `read failure clears stale totals and retry loads fresh totals`() = runBlocking {
        val stale = transaction("stale", TransactionType.INCOME, 10_000L, LocalDate.of(2026, 3, 2))
        val fresh = transaction("fresh", TransactionType.EXPENSE, 2_500L, LocalDate.of(2026, 3, 3))
        val repository = FakeTransactionRepository(listOf(stale))
        val viewModel = viewModel(repository)

        assertEquals(MonthTotals(incomeCents = 10_000L, expenseCents = 0L), content(viewModel.state.value).totals)

        repository.failRead(IllegalStateException("storage unavailable"))

        val failed = error(viewModel.state.value)
        assertTrue(failed.canRetry)
        assertEquals(MonthTotals(incomeCents = 0L, expenseCents = 0L), failed.totals)
        assertEquals(emptyList<Transaction>(), failed.recentActivity)

        repository.publish(listOf(fresh))
        viewModel.retry()

        val recovered = content(viewModel.state.value)
        assertEquals(MonthTotals(incomeCents = 0L, expenseCents = 2_500L), recovered.totals)
        assertEquals(-2_500L, recovered.totals.balanceCents)
        assertEquals(listOf(fresh), recovered.recentActivity)
    }

    private fun viewModel(repository: TransactionRepository) = DashboardViewModel(
        repository = repository,
        clock = clock,
        zone = zone,
        dispatcher = Dispatchers.Unconfined,
    )

    private fun transaction(id: String, type: TransactionType, amountCents: Long, localDate: LocalDate) = Transaction(
        id = id,
        type = type,
        amountCents = amountCents,
        categoryId = "category",
        localDate = localDate,
    )

    private fun content(state: DashboardUiState): DashboardUiState.Content {
        assertTrue("Expected dashboard content but was $state", state is DashboardUiState.Content)
        return state as DashboardUiState.Content
    }

    private fun empty(state: DashboardUiState): DashboardUiState.Empty {
        assertTrue("Expected an empty dashboard but was $state", state is DashboardUiState.Empty)
        return state as DashboardUiState.Empty
    }

    private fun error(state: DashboardUiState): DashboardUiState.Error {
        assertTrue("Expected a recoverable dashboard error but was $state", state is DashboardUiState.Error)
        return state as DashboardUiState.Error
    }

    private class FakeTransactionRepository(initial: List<Transaction>) : TransactionRepository {
        private val events = MutableSharedFlow<ReadEvent>(replay = 1)
        val observedMonths = mutableListOf<YearMonth>()

        init {
            publish(initial)
        }

        override fun observeMonth(month: YearMonth): Flow<List<Transaction>> {
            observedMonths += month
            return events.map { event ->
                when (event) {
                    is ReadEvent.Data -> event.transactions.filter { YearMonth.from(it.localDate) == month }
                    is ReadEvent.Failure -> throw event.error
                }
            }
        }

        fun publish(transactions: List<Transaction>) = publish(ReadEvent.Data(transactions))

        fun failRead(error: Throwable) = publish(ReadEvent.Failure(error))

        override suspend fun save(draft: TransactionDraft): Result<Transaction> =
            throw UnsupportedOperationException("Dashboard does not save transactions")

        override suspend fun delete(id: String): Result<Unit> =
            throw UnsupportedOperationException("Dashboard does not delete transactions")

        private fun publish(event: ReadEvent) {
            check(events.tryEmit(event))
        }
    }

    private sealed interface ReadEvent {
        data class Data(val transactions: List<Transaction>) : ReadEvent
        data class Failure(val error: Throwable) : ReadEvent
    }
}
