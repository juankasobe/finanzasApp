package com.saldoclaro.finance.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.core.presentation.UiErrorKey
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.data.repository.RoomFinanceRepositories
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class CategoryUiState(val categories: List<CategoryEntity> = emptyList(), val error: UiErrorKey? = null)

class CategoryViewModel(private val repositories: RoomFinanceRepositories) : ViewModel() {
    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repositories.observeCategories().collect { _state.value = CategoryUiState(it) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showError(UiErrorKey.DATA_UNAVAILABLE)
            }
        }
    }

    fun create(name: String, onSuccess: () -> Unit = {}) = viewModelScope.launch {
        repositories.createCategory(name).fold(
            onSuccess = { _state.value = _state.value.copy(error = null); onSuccess() },
            onFailure = { showError(UiErrorKey.OPERATION_FAILED) },
        )
    }

    fun archive(id: String) = viewModelScope.launch {
        repositories.archiveCustomCategory(id).fold(
            onSuccess = { _state.value = _state.value.copy(error = null) },
            onFailure = { showError(UiErrorKey.OPERATION_FAILED) },
        )
    }

    private fun showError(reason: UiErrorKey) {
        _state.value = _state.value.copy(error = reason)
    }
}
