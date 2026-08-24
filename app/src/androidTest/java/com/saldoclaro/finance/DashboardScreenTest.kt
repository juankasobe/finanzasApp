package com.saldoclaro.finance

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.feature.dashboard.DashboardScreen
import com.saldoclaro.finance.feature.dashboard.DashboardUiState
import com.saldoclaro.finance.feature.dashboard.DashboardViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<DashboardTestActivity>()

    private val zone = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:30:00Z"), zone)
    private val localCurrentMonth = YearMonth.of(2026, 3)

    @Test
    fun dashboardAnnouncesLoadingBeforeBothMonthSnapshots() {
        val viewModel = dashboard(ControlledTransactionRepository(), ControlledBudgetRepository())

        composeTestRule.setContent { DashboardScreen(viewModel) }

        composeTestRule.onNodeWithText("Loading dashboard").assertExists()
    }

    @Test
    fun dashboardExposesNoBudgetOverviewForCurrentMonthActivity() {
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val viewModel = dashboard(transactions, budgets)

        composeTestRule.setContent { DashboardScreen(viewModel) }
        transactions.publish(listOf(transaction("groceries-expense", TransactionType.EXPENSE, 2_500L)))
        composeTestRule.runOnIdle { check(viewModel.state.value is DashboardUiState.Loading) }
        budgets.publish(emptyList())

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithContentDescription("Income \$0.00").assertExists()
        composeTestRule.onNodeWithContentDescription("Expenses \$25.00").assertExists()
        composeTestRule.onNodeWithText("Budget overview").assertExists()
        composeTestRule.onNodeWithText("No budgets this month").assertExists()
    }

    @Test
    fun dashboardShowsProjectedProgressWhenBudgetExists() {
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val viewModel = dashboard(transactions, budgets)

        composeTestRule.setContent { DashboardScreen(viewModel) }
        transactions.publish(listOf(transaction("groceries-expense", TransactionType.EXPENSE, 2_500L)))
        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 3_000L)))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithText("Budget overview").assertExists()
        composeTestRule.onNodeWithText("Groceries").assertExists()
        composeTestRule.onNodeWithText("Under budget").assertExists()
        composeTestRule.onNodeWithText("\$25.00 of \$30.00").assertExists()
        composeTestRule.onNodeWithText("\$5.00 left").assertExists()
    }

    @Test
    fun retryClearsStaleDashboardValuesAndWaitsForFreshSnapshots() {
        val transactions = ControlledTransactionRepository()
        val budgets = ControlledBudgetRepository()
        val stale = transaction("stale-income", TransactionType.INCOME, 10_000L)
        val fresh = transaction("fresh-expense", TransactionType.EXPENSE, 2_500L)
        val viewModel = dashboard(transactions, budgets)

        composeTestRule.setContent { DashboardScreen(viewModel) }
        transactions.publish(listOf(stale))
        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 5_000L)))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithContentDescription("Income \$100.00").assertExists()

        transactions.failRead(IllegalStateException("storage unavailable"))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Error
        }
        composeTestRule.onNodeWithText("storage unavailable").assertExists()
        composeTestRule.onNodeWithContentDescription("Income \$100.00").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Total balance \$100.00").assertDoesNotExist()
        composeTestRule.onNodeWithText("Under budget").assertDoesNotExist()

        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.onNodeWithText("Loading dashboard").assertExists()
        transactions.publish(listOf(fresh))
        composeTestRule.runOnIdle { check(viewModel.state.value is DashboardUiState.Loading) }
        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 3_000L)))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithContentDescription("Income \$0.00").assertExists()
        composeTestRule.onNodeWithContentDescription("Expenses \$25.00").assertExists()
        composeTestRule.onNodeWithContentDescription("Total balance -\$25.00").assertExists()
    }

    private fun dashboard(
        transactions: TransactionRepository,
        budgets: BudgetRepository,
    ) = DashboardViewModel(
        transactionRepository = transactions,
        budgetRepository = budgets,
        clock = clock,
        zone = zone,
        dispatcher = Dispatchers.Main.immediate,
    )

    private fun transaction(id: String, type: TransactionType, amountCents: Long) = Transaction(
        id = id,
        type = type,
        amountCents = amountCents,
        categoryId = "groceries",
        localDate = LocalDate.of(2026, 3, 15),
    )

    private class ControlledTransactionRepository : TransactionRepository {
        private val events = MutableSharedFlow<ReadEvent>(extraBufferCapacity = 16)

        override fun observeMonth(month: YearMonth): Flow<List<Transaction>> = events.map { event ->
            when (event) {
                is ReadEvent.Data -> event.transactions.filter { YearMonth.from(it.localDate) == month }
                is ReadEvent.Failure -> throw event.error
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

        override fun observeMonth(month: YearMonth): Flow<List<Budget>> = events.map { event ->
            when (event) {
                is BudgetReadEvent.Data -> event.budgets.filter { it.month == month }
                is BudgetReadEvent.Failure -> throw event.error
            }
        }

        fun publish(budgets: List<Budget>) = events.tryEmit(BudgetReadEvent.Data(budgets))

        override suspend fun save(categoryId: String, month: YearMonth, limitCents: Long): Result<Unit> =
            Result.success(Unit)
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
