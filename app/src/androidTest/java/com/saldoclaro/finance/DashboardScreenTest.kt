package com.saldoclaro.finance

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
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
    val composeTestRule = createComposeRule()

    private val zone = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:30:00Z"), zone)

    @Test
    fun dashboardAnnouncesLoadingBeforeItsFirstMonthSnapshot() {
        val viewModel = dashboard(ControlledTransactionRepository())

        composeTestRule.setContent { DashboardScreen(viewModel) }

        composeTestRule.onNodeWithText("Loading dashboard").assertExists()
    }

    @Test
    fun dashboardExposesNoBudgetOverviewForCurrentMonthActivity() {
        val repository = ControlledTransactionRepository().apply {
            publish(
                listOf(
                    transaction(
                        id = "groceries-expense",
                        type = TransactionType.EXPENSE,
                        amountCents = 2_500L,
                    ),
                ),
            )
        }
        val viewModel = dashboard(repository)

        composeTestRule.setContent { DashboardScreen(viewModel) }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithText("Budget overview").assertExists()
        composeTestRule.onNodeWithText("No budgets this month").assertExists()
    }

    @Test
    fun retryClearsStaleTotalsBeforeItDisplaysFreshDashboardValues() {
        val stale = transaction(
            id = "stale-income",
            type = TransactionType.INCOME,
            amountCents = 10_000L,
        )
        val fresh = transaction(
            id = "fresh-expense",
            type = TransactionType.EXPENSE,
            amountCents = 2_500L,
        )
        val repository = ControlledTransactionRepository().apply { publish(listOf(stale)) }
        val viewModel = dashboard(repository)

        composeTestRule.setContent { DashboardScreen(viewModel) }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithText("Income: 10000 cents").assertExists()

        repository.failRead(IllegalStateException("storage unavailable"))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Error
        }
        composeTestRule.onNodeWithText("storage unavailable").assertExists()
        composeTestRule.onNodeWithText("Income: 10000 cents").assertDoesNotExist()
        composeTestRule.onNodeWithText("Balance: 10000 cents").assertDoesNotExist()

        repository.publish(listOf(fresh))
        composeTestRule.onNodeWithText("Retry").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithText("Income: 0 cents").assertExists()
        composeTestRule.onNodeWithText("Expenses: 2500 cents").assertExists()
        composeTestRule.onNodeWithText("Balance: -2500 cents").assertExists()
    }

    private fun dashboard(repository: TransactionRepository) = DashboardViewModel(
        repository = repository,
        clock = clock,
        zone = zone,
        dispatcher = Dispatchers.Main.immediate,
    )

    private fun transaction(
        id: String,
        type: TransactionType,
        amountCents: Long,
    ) = Transaction(
        id = id,
        type = type,
        amountCents = amountCents,
        categoryId = "groceries",
        localDate = LocalDate.of(2026, 3, 15),
    )

    private class ControlledTransactionRepository : TransactionRepository {
        private val events = MutableSharedFlow<ReadEvent>(replay = 1)

        override fun observeMonth(month: YearMonth): Flow<List<Transaction>> = events.map { event ->
            when (event) {
                is ReadEvent.Data -> event.transactions.filter { YearMonth.from(it.localDate) == month }
                is ReadEvent.Failure -> throw event.error
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
