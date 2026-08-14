package com.saldoclaro.finance.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.repository.TransactionRepository
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
enum class TransactionField { TYPE, AMOUNT, CATEGORY, DATE }

sealed interface TransactionUiState {
    data object Empty : TransactionUiState
    data class Content(val transactions: List<Transaction>) : TransactionUiState
    data class Validation(val invalidFields: Set<TransactionField>, val transactions: List<Transaction>) : TransactionUiState
    data class ConfirmDelete(val transaction: Transaction) : TransactionUiState
    data class Error(val message: String, val transactions: List<Transaction>, val canRetry: Boolean = true) : TransactionUiState
}

private sealed interface TransactionMutation {
    data class Save(val draft: TransactionDraft) : TransactionMutation
    data class Delete(val id: String) : TransactionMutation
}

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val month: YearMonth,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val activity = MutableStateFlow(emptyList<Transaction>())
    private val retryMutation = MutableStateFlow<TransactionMutation?>(null)
    private val _state = MutableStateFlow<TransactionUiState>(TransactionUiState.Empty)
    private var observation: Job? = null
    val state: StateFlow<TransactionUiState> = _state.asStateFlow()
    init {
        observeMonth()
    }

    private fun observeMonth() {
        observation?.cancel()
        observation = viewModelScope.launch(dispatcher) {
            try {
                repository.observeMonth(month).collect { records ->
                    activity.value = records.toList()
                    if (_state.value !is TransactionUiState.ConfirmDelete && _state.value !is TransactionUiState.Error) showActivity()
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showError(error)
            }
        }
    }
    fun save(draft: TransactionDraft) {
        draft.invalidFields().takeIf { it.isNotEmpty() }?.let {
            retryMutation.value = null
            _state.value = TransactionUiState.Validation(it, activity.value)
            return
        }
        mutate(TransactionMutation.Save(draft))
    }

    fun requestDelete(id: String) = activity.value.firstOrNull { it.id == id }
        ?.let { _state.value = TransactionUiState.ConfirmDelete(it) }
    fun cancelDelete() = showActivity()
    fun confirmDelete() = (_state.value as? TransactionUiState.ConfirmDelete)
        ?.let { mutate(TransactionMutation.Delete(it.transaction.id)) }
    fun retry() = retryMutation.value?.let(::mutate) ?: observeMonth()

    private fun mutate(mutation: TransactionMutation) {
        retryMutation.value = mutation
        viewModelScope.launch(dispatcher) {
            execute(mutation).fold(
                onSuccess = { retryMutation.value = null; showActivity() },
                onFailure = ::showError,
            )
        }
    }

    private suspend fun execute(mutation: TransactionMutation): Result<*> = when (mutation) {
        is TransactionMutation.Save -> repository.save(mutation.draft)
        is TransactionMutation.Delete -> repository.delete(mutation.id)
    }

    private fun showActivity() { _state.value = activity.value.asActivityState() }
    private fun showError(error: Throwable) { _state.value = TransactionUiState.Error(error.message ?: "Transaction operation failed", activity.value) }
}

private fun List<Transaction>.asActivityState(): TransactionUiState =
    if (isEmpty()) TransactionUiState.Empty else TransactionUiState.Content(this)

private fun TransactionDraft.invalidFields(): Set<TransactionField> = buildSet {
    if (type == null) add(TransactionField.TYPE)
    if (!amount.isPositiveWholeCents()) add(TransactionField.AMOUNT)
    if (categoryId.isNullOrBlank()) add(TransactionField.CATEGORY)
    if (localDate == null) add(TransactionField.DATE)
}
private fun String.isPositiveWholeCents() = runCatching { trim().toBigDecimal().movePointRight(2).longValueExact() > 0 }.getOrDefault(false)
