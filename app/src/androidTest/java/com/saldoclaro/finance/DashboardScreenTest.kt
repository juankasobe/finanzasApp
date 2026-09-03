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
import com.saldoclaro.finance.domain.repository.BudgetTarget
import com.saldoclaro.finance.domain.repository.DeleteEvidence
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

        composeTestRule.onNodeWithText("Cargando panel").assertExists()
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
        composeTestRule.onNodeWithContentDescription("Ingresos 0,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithContentDescription("Gastos 25,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithText("Resumen de presupuestos").assertExists()
        composeTestRule.onNodeWithText("Gastado 25,00\u00a0US$ sin límite mensual.").assertExists()
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
        composeTestRule.onNodeWithText("Resumen de presupuestos").assertExists()
        composeTestRule.onNodeWithText("Supermercado").assertExists()
        composeTestRule.onNodeWithText("Dentro del límite").assertExists()
        composeTestRule.onNodeWithText("25,00\u00a0US\$ de 30,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithText("Quedan 5,00\u00a0US\$").assertExists()
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
        composeTestRule.onNodeWithContentDescription("Ingresos 100,00\u00a0US\$").assertExists()

        transactions.failRead(IllegalStateException("storage unavailable"))
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Error
        }
        composeTestRule.onNodeWithText("No se pudieron cargar los datos.").assertExists()
        composeTestRule.onNodeWithText("storage unavailable").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Ingresos 100,00\u00a0US\$").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Saldo total 100,00\u00a0US\$").assertDoesNotExist()
        composeTestRule.onNodeWithText("Dentro del límite").assertDoesNotExist()

        composeTestRule.onNodeWithText("Reintentar").performClick()
        composeTestRule.onNodeWithText("Cargando panel").assertExists()
        transactions.publish(listOf(fresh))
        composeTestRule.runOnIdle { check(viewModel.state.value is DashboardUiState.Loading) }
        budgets.publish(listOf(Budget("groceries", localCurrentMonth, 3_000L)))

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value is DashboardUiState.Content
        }
        composeTestRule.onNodeWithContentDescription("Ingresos 0,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithContentDescription("Gastos 25,00\u00a0US\$").assertExists()
        composeTestRule.onNodeWithContentDescription("Saldo total -25,00\u00a0US\$").assertExists()
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
