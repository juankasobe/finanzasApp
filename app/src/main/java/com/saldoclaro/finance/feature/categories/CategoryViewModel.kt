package com.saldoclaro.finance.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saldoclaro.finance.core.presentation.UiErrorKey
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.data.repository.CategoryDeleteOutcome
import com.saldoclaro.finance.data.repository.CategoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class CategoryUiState(val categories: List<CategoryEntity> = emptyList(), val error: UiErrorKey? = null)

enum class CategoryMutation { Rename, Delete, Archive }

sealed interface CategoryMutationState {
    data object Idle : CategoryMutationState
    data class Editing(val category: CategoryEntity) : CategoryMutationState
    data class ConfirmDelete(val category: CategoryEntity) : CategoryMutationState
    data class InUse(val category: CategoryEntity) : CategoryMutationState
    data class Error(val category: CategoryEntity? = null) : CategoryMutationState
    data class Succeeded(val operation: CategoryMutation) : CategoryMutationState
}

class CategoryViewModel(
    private val repositories: CategoryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()
    private val _mutationState = MutableStateFlow<CategoryMutationState>(CategoryMutationState.Idle)
    val mutationState: StateFlow<CategoryMutationState> = _mutationState.asStateFlow()

    init {
        viewModelScope.launch(dispatcher) {
            try {
                repositories.observeCategories().collect { _state.value = CategoryUiState(it) }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                showError(UiErrorKey.DATA_UNAVAILABLE)
            }
        }
    }

    fun create(name: String, onSuccess: () -> Unit = {}) = viewModelScope.launch(dispatcher) {
        repositories.createCategory(name).fold(
            onSuccess = { _state.value = _state.value.copy(error = null); onSuccess() },
            onFailure = { showError(UiErrorKey.OPERATION_FAILED) },
        )
    }

    fun openEdit(category: CategoryEntity) {
        _mutationState.value = if (category.isBuiltIn) CategoryMutationState.Error(category)
        else CategoryMutationState.Editing(category)
    }

    fun submitRename(name: String) {
        val target = (_mutationState.value as? CategoryMutationState.Editing)?.category ?: return
        viewModelScope.launch(dispatcher) {
            repositories.renameCustomCategory(target.id, name).fold(
                onSuccess = { _mutationState.value = CategoryMutationState.Succeeded(CategoryMutation.Rename) },
                onFailure = { _mutationState.value = CategoryMutationState.Error(target) },
            )
        }
    }

    fun requestDelete(category: CategoryEntity) {
        _mutationState.value = if (category.isBuiltIn) CategoryMutationState.Error(category)
        else CategoryMutationState.ConfirmDelete(category)
    }

    fun confirmDelete() {
        val target = (_mutationState.value as? CategoryMutationState.ConfirmDelete)?.category ?: return
        viewModelScope.launch(dispatcher) {
            repositories.deleteCustomCategory(target.id).fold(
                onSuccess = { outcome ->
                    _mutationState.value = when (outcome) {
                        CategoryDeleteOutcome.Deleted -> CategoryMutationState.Succeeded(CategoryMutation.Delete)
                        CategoryDeleteOutcome.InUse -> CategoryMutationState.InUse(target)
                    }
                },
                onFailure = { _mutationState.value = CategoryMutationState.Error(target) },
            )
        }
    }

    fun dismissMutation() { _mutationState.value = CategoryMutationState.Idle }

    fun archive(id: String) = viewModelScope.launch(dispatcher) {
        repositories.archiveCustomCategory(id).fold(
            onSuccess = {
                _state.value = _state.value.copy(error = null)
                _mutationState.value = CategoryMutationState.Succeeded(CategoryMutation.Archive)
            },
            onFailure = {
                showError(UiErrorKey.OPERATION_FAILED)
                _mutationState.value = CategoryMutationState.Error()
            },
        )
    }

    private fun showError(reason: UiErrorKey) {
        _state.value = _state.value.copy(error = reason)
    }
}
