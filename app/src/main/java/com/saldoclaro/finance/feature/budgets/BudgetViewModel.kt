package com.saldoclaro.finance.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.core.presentation.UiErrorKey
import com.saldoclaro.finance.core.time.CurrentMonthSource
import com.saldoclaro.finance.core.time.SystemCurrentMonthSource
import com.saldoclaro.finance.domain.repository.BudgetMutationError
import com.saldoclaro.finance.domain.repository.BudgetMutationException
import com.saldoclaro.finance.domain.repository.BudgetRepository
import com.saldoclaro.finance.domain.repository.BudgetTarget
import com.saldoclaro.finance.domain.repository.TransactionRepository
import com.saldoclaro.finance.domain.usecase.BudgetProgressItem
import com.saldoclaro.finance.domain.usecase.BudgetState
import com.saldoclaro.finance.domain.usecase.currentMonth
import com.saldoclaro.finance.domain.usecase.projectBudgetProgress
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

enum class BudgetField { LIMIT }

sealed interface BudgetUiState {
    data class Content(val progress: List<BudgetProgressItem>) : BudgetUiState
    data class Validation(val invalidFields: Set<BudgetField>) : BudgetUiState
    data class Error(val reason: UiErrorKey, val progress: List<BudgetProgressItem>, val canRetry: Boolean = true) : BudgetUiState
}

sealed interface BudgetMutationState {
    data object Idle : BudgetMutationState
    data class Editing(val target: BudgetTarget) : BudgetMutationState
    data class ConfirmDelete(val target: BudgetTarget) : BudgetMutationState
    data class Running(val target: BudgetTarget) : BudgetMutationState
    data class Error(val target: BudgetTarget, val reason: UiErrorKey) : BudgetMutationState
}

private data class Mutation(val target: BudgetTarget, val limitCents: Long? = null)

private data class SaveLimit(val categoryId: String, val month: java.time.YearMonth, val limitCents: Long)

class BudgetViewModel(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    clock: Clock,
    zone: ZoneId,
    private val monthSource: CurrentMonthSource = SystemCurrentMonthSource(clock, zone),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val clock = clock
    private val zone = zone
    private val _state = MutableStateFlow<BudgetUiState>(BudgetUiState.Content(emptyList()))
    private val _mutationState = MutableStateFlow<BudgetMutationState>(BudgetMutationState.Idle)
    private var observation: Job? = null
    private var pendingSave: SaveLimit? = null
    private var pendingMutation: Mutation? = null
    val state: StateFlow<BudgetUiState> = _state.asStateFlow()
    val mutationState: StateFlow<BudgetMutationState> = _mutationState.asStateFlow()

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
        save(SaveLimit(categoryId, currentMonth(clock, zone), limitCents))
    }

    fun openTarget(target: BudgetTarget) {
        pendingMutation = null
        _mutationState.value = BudgetMutationState.Editing(target)
    }

    fun submitEdit(limit: String) {
        val target = (_mutationState.value as? BudgetMutationState.Editing)?.target ?: return
        val cents = limit.toPositiveCentsOrNull()
        if (cents == null) {
            _mutationState.value = BudgetMutationState.Error(target, UiErrorKey.INVALID_AMOUNT)
            return
        }
        mutate(Mutation(target, cents))
    }

    fun requestDelete() {
        (_mutationState.value as? BudgetMutationState.Editing)?.let {
            _mutationState.value = BudgetMutationState.ConfirmDelete(it.target)
        }
    }

    fun cancelDelete() {
        (_mutationState.value as? BudgetMutationState.ConfirmDelete)?.let {
            _mutationState.value = BudgetMutationState.Editing(it.target)
        }
    }

    fun confirmDelete() {
        (_mutationState.value as? BudgetMutationState.ConfirmDelete)?.let { mutate(Mutation(it.target)) }
    }

    fun cancelManagement() {
        pendingMutation = null
        _mutationState.value = BudgetMutationState.Idle
    }

    fun retry() = pendingMutation?.let(::mutate) ?: pendingSave?.let(::save) ?: observeMonth()

    private fun save(command: SaveLimit) {
        pendingSave = command
        viewModelScope.launch(dispatcher) {
            budgetRepository.save(command.categoryId, command.month, command.limitCents).fold(
                onSuccess = { pendingSave = null },
                onFailure = { showError(UiErrorKey.OPERATION_FAILED, canRetry = true) },
            )
        }
    }

    private fun observeMonth() {
        observation?.cancel()
        observation = viewModelScope.launch(dispatcher) {
            try {
                monthSource.month
                    .flatMapLatest { month -> flow {
                        budgetRepository.rollover(month.minusMonths(1), month).getOrThrow()
                        emitAll(combine(transactionRepository.observeMonth(month), budgetRepository.observeMonth(month)) { transactions, budgets ->
                            projectBudgetProgress(transactions, budgets)
                        })
                    } }
                    .collect { _state.value = BudgetUiState.Content(it) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showError(UiErrorKey.DATA_UNAVAILABLE, canRetry = true)
            }
        }
    }

    private fun mutate(command: Mutation) {
        pendingMutation = command
        viewModelScope.launch(dispatcher) {
            if (command.target.month != currentMonth(clock, zone) || command.target.month != monthSource.month.value) {
                _mutationState.value = BudgetMutationState.Error(command.target, UiErrorKey.TARGET_UNAVAILABLE)
                return@launch
            }
            _mutationState.value = BudgetMutationState.Running(command.target)
            execute(command).fold(
                onSuccess = { pendingMutation = null; _mutationState.value = BudgetMutationState.Idle },
                onFailure = { _mutationState.value = BudgetMutationState.Error(command.target, it.toUiErrorKey()) },
            )
        }
    }

    private suspend fun execute(command: Mutation) = command.limitCents?.let {
        budgetRepository.editAmount(command.target, it)
    } ?: budgetRepository.delete(command.target).map { Unit }

    private fun showError(reason: UiErrorKey, canRetry: Boolean) {
        _state.value = BudgetUiState.Error(reason, emptyList(), canRetry)
    }
}

private fun Throwable.toUiErrorKey() = when ((this as? BudgetMutationException)?.reason) {
    BudgetMutationError.InvalidLimit -> UiErrorKey.INVALID_AMOUNT
    BudgetMutationError.TargetMissing, BudgetMutationError.TargetStale -> UiErrorKey.TARGET_UNAVAILABLE
    else -> UiErrorKey.OPERATION_FAILED
}

private fun String.toPositiveCentsOrNull(): Long? {
    val value = trim()
    if (!value.matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) return null
    return runCatching { value.toBigDecimal().movePointRight(2).longValueExact().takeIf { it > 0 } }.getOrNull()
}
