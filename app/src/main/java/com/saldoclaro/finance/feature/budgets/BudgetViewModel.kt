package com.saldoclaro.finance.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.domain.model.Budget
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.domain.usecase.BudgetState
import com.saldoclaro.finance.domain.usecase.calculateBudgetProgress
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class BudgetField { LIMIT }

data class BudgetProgressItem(
    val categoryId: String,
    val state: BudgetState,
    val limitCents: Long?,
    val spentCents: Long,
    val remainingCents: Long?,
)

sealed interface BudgetUiState {
    data class Content(val progress: List<BudgetProgressItem>) : BudgetUiState
    data class Validation(val invalidFields: Set<BudgetField>) : BudgetUiState
    data class Error(val message: String, val progress: List<BudgetProgressItem>, val canRetry: Boolean = true) : BudgetUiState
}

private data class SaveLimit(val categoryId: String, val limitCents: Long)

class BudgetViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    clock: Clock,
    zone: ZoneId,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val month = currentMonth(clock, zone)
    private val _state = MutableStateFlow<BudgetUiState>(BudgetUiState.Content(emptyList()))
    private var observation: Job? = null
    private var pendingSave: SaveLimit? = null
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()

    init {
        observeMonth()
    }

    fun saveLimit(categoryId: String, limit: String) {
        val limitCents = limit.toPositiveCentsOrNull()
        if (limitCents == null) {
            pendingSave = null
            _state.value = BudgetUiState.Validation(setOf(BudgetField.LIMIT))
            return
        }
        save(SaveLimit(categoryId, limitCents))
    }

    fun retry() = pendingSave?.let(::save) ?: observeMonth()

    private fun save(command: SaveLimit) {
        pendingSave = command
        viewModelScope.launch(dispatcher) {
            budgetRepository.save(command.categoryId, month, command.limitCents).fold(
                onSuccess = { pendingSave = null },
                onFailure = { error -> showError(error, canRetry = true) },
            )
        }
    }

    private fun observeMonth() {
        observation?.cancel()
        observation = viewModelScope.launch(dispatcher) {
            try {
                combine(transactionRepository.observeMonth(month), budgetRepository.observeMonth(month)) { transactions, budgets ->
                    transactions.toProgress(budgets)
                }.collect { _state.value = BudgetUiState.Content(it) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showError(error, canRetry = true)
            }
        }
    }

    private fun showError(error: Throwable, canRetry: Boolean) {
        _state.value = BudgetUiState.Error(error.message ?: "Budget data unavailable", emptyList(), canRetry)
    }
}

private fun List<Transaction>.toProgress(budgets: List<Budget>): List<BudgetProgressItem> {
    val spent = filter { it.type == TransactionType.EXPENSE }.groupBy { it.categoryId }
        .mapValues { (_, records) -> records.sumOf { it.amountCents } }
    val limits = budgets.associateBy { it.categoryId }
    return (spent.keys + limits.keys).sorted().map { categoryId ->
        val limitCents = limits[categoryId]?.limitCents
        val spentCents = spent[categoryId] ?: 0L
        val progress = calculateBudgetProgress(limitCents, spentCents)
        BudgetProgressItem(categoryId, progress.state, limitCents, spentCents, progress.remainingCents)
    }
}

private fun String.toPositiveCentsOrNull(): Long? {
    val value = trim()
    if (!value.matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) return null
    return runCatching { value.toBigDecimal().movePointRight(2).longValueExact().takeIf { it > 0 } }.getOrNull()
}
