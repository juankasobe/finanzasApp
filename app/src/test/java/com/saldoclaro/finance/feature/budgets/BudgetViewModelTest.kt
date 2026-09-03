package com.saldoclaro.finance.feature.budgets

import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.core.presentation.UiErrorKey
import com.saldoclaro.finance.core.time.CurrentMonthSource
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.BudgetMutationError
import com.saldoclaro.finance.domain.repository.BudgetMutationException
import com.saldoclaro.finance.domain.repository.BudgetTarget
import com.saldoclaro.finance.domain.repository.DeleteEvidence
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.domain.usecase.BudgetProgressItem
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
                transaction("income-only", TransactionType.INCOME, 25_000L, "income-only"),
                transaction("other-month", TransactionType.EXPENSE, 90_000L, "under", LocalDate.of(2026, 4, 1)),
            ),
        )
        val budgets = FakeBudgetRepository(
            listOf(budget("under", 10_000L), budget("at-limit", 4_000L), budget("over", 6_000L)),
        )

        val progress = content(viewModel(transactions, budgets).state.value).progress.associateBy { it.categoryId }

        assertEquals(localCurrentMonth, transactions.observedMonths.single())
        assertEquals(localCurrentMonth, budgets.observedMonths.single())
        assertEquals(listOf("at-limit", "no-budget", "over", "under"), progress.keys.toList())
        assertTrue("Income-only categories must not create progress", "income-only" !in progress)
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

    @Test
    fun `month refresh rolls active limits without replacing current or archived budgets`() = runBlocking {
        val next = localCurrentMonth.plusMonths(1)
        val transactions = FakeTransactionRepository(listOf(
            transaction("active-spend", TransactionType.EXPENSE, 2_000L, "active", next.atDay(4)),
            transaction("archived-spend", TransactionType.EXPENSE, 1_000L, "archived", next.atDay(5)),
        ))
        val budgets = FakeBudgetRepository(listOf(
            budget("active", 5_000L),
            budget("archived", 6_000L),
            budget("existing", 3_000L),
            budget("existing", 8_000L, next),
        ))
        budgets.archivedCategories = setOf("archived")
        val source = TestMonthSource(localCurrentMonth)
        val viewModel = viewModel(transactions, budgets, source)

        source.advance(next)
        source.refresh()

        val progress = content(viewModel.state.value).progress.associateBy { it.categoryId }
        assertProgress(progress.getValue("active"), BudgetState.UNDER, 5_000L, 2_000L, 3_000L)
        assertProgress(progress.getValue("archived"), BudgetState.NO_BUDGET, null, 1_000L, null)
        assertProgress(progress.getValue("existing"), BudgetState.UNDER, 8_000L, 0L, 8_000L)
        assertEquals(next, transactions.observedMonths.last())
        assertEquals(next, budgets.observedMonths.last())
        assertEquals(1, budgets.records.count { it.categoryId == "active" && it.month == next })
        assertEquals(1, budgets.rollovers.count { it.second == next })
    }

    @Test
    fun `archived edit fails delete succeeds and stale target remains recoverable`() = runBlocking {
        val target = BudgetTarget("archived", localCurrentMonth, 5_000L)
        val budgets = FakeBudgetRepository(listOf(budget("archived", 5_000L)))
        budgets.editResult = Result.failure(BudgetMutationException(BudgetMutationError.ArchivedCategory))
        budgets.deleteResult = Result.success(DeleteEvidence(1))
        val viewModel = viewModel(FakeTransactionRepository(emptyList()), budgets)

        viewModel.openTarget(target)
        viewModel.submitEdit("60.00")
        assertEquals(BudgetMutationState.Error(target, UiErrorKey.OPERATION_FAILED), viewModel.mutationState.value)

        viewModel.openTarget(target)
        viewModel.requestDelete()
        assertEquals(BudgetMutationState.ConfirmDelete(target), viewModel.mutationState.value)
        viewModel.confirmDelete()
        assertEquals(BudgetMutationState.Idle, viewModel.mutationState.value)
        assertEquals(1, budgets.deleteCalls)

        val stale = target.copy(month = localCurrentMonth.minusMonths(1))
        viewModel.openTarget(stale)
        viewModel.submitEdit("70.00")
        assertEquals(BudgetMutationState.Error(stale, UiErrorKey.TARGET_UNAVAILABLE), viewModel.mutationState.value)
        assertEquals(1, budgets.editCalls)
    }

    private fun viewModel(
        transactions: TransactionRepository,
        budgets: BudgetRepository,
        monthSource: CurrentMonthSource = TestMonthSource(localCurrentMonth),
    ) = BudgetViewModel(
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
        categoryId: String,
        localDate: LocalDate = LocalDate.of(2026, 3, 15),
    ) = Transaction(id, type, amountCents, categoryId, localDate)

    private fun budget(categoryId: String, limitCents: Long, month: YearMonth = localCurrentMonth) = Budget(categoryId, month, limitCents)

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

    private class TestMonthSource(initial: YearMonth) : CurrentMonthSource {
        override val month = MutableStateFlow(initial)
        private var pending = initial
        fun advance(next: YearMonth) { pending = next }
        override fun refresh() { month.value = pending }
        override fun setForeground(active: Boolean) = Unit
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
        var records = initial
        private val events = ReadEvents(initial)
        val observedMonths = mutableListOf<YearMonth>()
        val rollovers = mutableListOf<Pair<YearMonth, YearMonth>>()
        var archivedCategories = emptySet<String>()
        var saveCalls = 0
        var editCalls = 0
        var deleteCalls = 0
        var editResult: Result<Unit> = Result.failure(IllegalStateException("Budget management is outside this test"))
        var deleteResult: Result<DeleteEvidence> = Result.failure(IllegalStateException("Budget management is outside this test"))

        override fun observeMonth(month: YearMonth): Flow<List<Budget>> {
            observedMonths += month
            return events.observe { it.month == month }
        }

        override suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit> {
            saveCalls += 1
            return Result.success(Unit)
        }

        override suspend fun rollover(from: YearMonth, to: YearMonth): Result<Unit> {
            rollovers += from to to
            val current = records.filter { it.month == to }.map { it.categoryId }.toSet()
            val copies = records.filter { it.month == from && it.categoryId !in archivedCategories && it.categoryId !in current }
                .map { it.copy(month = to) }
            if (copies.isNotEmpty()) publish(records + copies)
            return Result.success(Unit)
        }

        override suspend fun editAmount(target: BudgetTarget, newLimitCents: Long): Result<Unit> {
            editCalls += 1
            return editResult
        }

        override suspend fun delete(target: BudgetTarget): Result<DeleteEvidence> {
            deleteCalls += 1
            return deleteResult
        }

        fun publish(budgets: List<Budget>) { records = budgets; events.publish(budgets) }
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
