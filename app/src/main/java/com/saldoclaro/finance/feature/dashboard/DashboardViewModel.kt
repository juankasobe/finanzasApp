package com.saldoclaro.finance.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.domain.usecase.BudgetProgressItem
import com.saldoclaro.finance.domain.usecase.calculateMonthTotals
import com.saldoclaro.finance.domain.usecase.currentMonth
import com.saldoclaro.finance.domain.usecase.projectBudgetProgress
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

sealed interface DashboardBudgetOverview {
    data object NoBudgets : DashboardBudgetOverview
    data class Progress(val items: List<BudgetProgressItem>) : DashboardBudgetOverview
}

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Content(
        val totals: MonthTotals,
        val recentActivity: List<Transaction>,
        val budgetOverview: DashboardBudgetOverview,
    ) : DashboardUiState
    data class Error(val message: String, val canRetry: Boolean = true) : DashboardUiState
}

class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    clock: Clock,
    zone: ZoneId,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val month = currentMonth(clock, zone)
    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    private var observation: Job? = null
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        observeMonth()
    }

    fun retry() = observeMonth()

    private fun observeMonth() {
        observation?.cancel()
        _state.value = DashboardUiState.Loading
        observation = viewModelScope.launch(dispatcher) {
            try {
                combine(
                    transactionRepository.observeMonth(month),
                    budgetRepository.observeMonth(month),
                ) { transactions, budgets ->
                    transactions.toDashboardContent(month, budgets)
                }.collect { _state.value = it }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _state.value = DashboardUiState.Error(error.message ?: "Dashboard data unavailable")
            }
        }
    }
}

private fun List<Transaction>.toDashboardContent(
    month: YearMonth,
    budgets: List<Budget>,
): DashboardUiState.Content {
    val monthBounds = month.atDay(1)..month.atEndOfMonth()
    val currentTransactions = filter { it.localDate in monthBounds }
    val currentBudgets = budgets.filter { it.month == month }
    return DashboardUiState.Content(
        totals = calculateMonthTotals(currentTransactions, monthBounds),
        recentActivity = currentTransactions,
        budgetOverview = if (currentBudgets.isEmpty()) {
            DashboardBudgetOverview.NoBudgets
        } else {
            DashboardBudgetOverview.Progress(projectBudgetProgress(currentTransactions, currentBudgets))
        },
    )
}
