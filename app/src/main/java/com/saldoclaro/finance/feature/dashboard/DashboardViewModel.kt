package com.saldoclaro.finance.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.domain.usecase.currentMonth
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data class Content(val totals: MonthTotals, val recentActivity: List<Transaction>) : DashboardUiState
    data class Empty(val totals: MonthTotals, val recentActivity: List<Transaction>) : DashboardUiState
    data class Error(
        val message: String,
        val totals: MonthTotals,
        val recentActivity: List<Transaction>,
        val canRetry: Boolean = true,
    ) : DashboardUiState
}

class DashboardViewModel(
    private val repository: TransactionRepository,
    clock: Clock,
    zone: ZoneId,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val month = currentMonth(clock, zone)
    private val _state = MutableStateFlow<DashboardUiState>(emptyState())
    private var observation: Job? = null
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        observeMonth()
    }

    fun retry() = observeMonth()

    private fun observeMonth() {
        observation?.cancel()
        observation = viewModelScope.launch(dispatcher) {
            try {
                repository.observeMonth(month).collect { transactions ->
                    _state.value = transactions.asDashboardState()
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _state.value = errorState(error)
            }
        }
    }
}

private fun List<Transaction>.asDashboardState(): DashboardUiState {
    val totals = MonthTotals(
        incomeCents = filter { it.type == TransactionType.INCOME }.sumOf { it.amountCents },
        expenseCents = filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountCents },
    )
    return if (isEmpty()) DashboardUiState.Empty(totals, emptyList()) else DashboardUiState.Content(totals, toList())
}

private fun emptyState() = DashboardUiState.Empty(MonthTotals(0L, 0L), emptyList())

private fun errorState(error: Throwable) = DashboardUiState.Error(
    message = error.message ?: "Dashboard data unavailable",
    totals = MonthTotals(0L, 0L),
    recentActivity = emptyList(),
)
