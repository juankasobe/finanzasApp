package com.saldoclaro.finance.feature.categories

import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.data.repository.CategoryDeleteOutcome
import com.saldoclaro.finance.data.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryViewModelTest {
    private val custom = category("custom-travel", "Travel")
    private val archived = category("custom-old", "Old", isArchived = true)
    private val builtIn = category("builtin-groceries", "Supermercado", isBuiltIn = true)

    @Test
    fun `rename success preserves identity and archive state`() = runBlocking {
        val repository = FakeCategoryRepository(listOf(custom, archived))
        val viewModel = viewModel(repository)

        viewModel.openEdit(archived)
        viewModel.submitRename("Renamed")

        assertEquals(CategoryMutationState.Succeeded(CategoryMutation.Rename), viewModel.mutationState.value)
        assertEquals(archived.copy(name = "Renamed", normalizedName = "renamed"), repository.categories.value.single { it.id == archived.id })
    }

    @Test
    fun `blank duplicate and built-in rename expose deterministic errors`() = runBlocking {
        val repository = FakeCategoryRepository(listOf(custom, builtIn))
        val viewModel = viewModel(repository)

        listOf(" ", "Supermercado").forEach { name ->
            viewModel.openEdit(custom)
            viewModel.submitRename(name)
            assertEquals(CategoryMutationState.Error(custom), viewModel.mutationState.value)
        }
        viewModel.openEdit(builtIn)
        assertEquals(CategoryMutationState.Error(builtIn), viewModel.mutationState.value)
        assertEquals(emptyList<String>(), repository.renamedIds.filter { it == builtIn.id })
    }

    @Test
    fun `delete success and in-use result remain distinct`() = runBlocking {
        val repository = FakeCategoryRepository(listOf(custom, archived))
        val viewModel = viewModel(repository)

        viewModel.requestDelete(custom)
        viewModel.confirmDelete()
        assertEquals(CategoryMutationState.Succeeded(CategoryMutation.Delete), viewModel.mutationState.value)

        repository.deleteOutcome = CategoryDeleteOutcome.InUse
        viewModel.requestDelete(archived)
        viewModel.confirmDelete()
        assertEquals(CategoryMutationState.InUse(archived), viewModel.mutationState.value)
    }

    @Test
    fun `built-in archive and delete fail without mutation`() = runBlocking {
        val repository = FakeCategoryRepository(listOf(builtIn))
        val viewModel = viewModel(repository)

        viewModel.archive(builtIn.id)
        assertEquals(CategoryMutationState.Error(), viewModel.mutationState.value)
        viewModel.requestDelete(builtIn)
        assertEquals(CategoryMutationState.Error(builtIn), viewModel.mutationState.value)
        assertEquals(0, repository.deleteCalls)
    }

    private fun viewModel(repository: CategoryRepository) =
        CategoryViewModel(repository, Dispatchers.Unconfined)

    private fun category(id: String, name: String, isBuiltIn: Boolean = false, isArchived: Boolean = false) =
        CategoryEntity(id, name, name.lowercase(), isBuiltIn, isArchived)

    private class FakeCategoryRepository(initial: List<CategoryEntity>) : CategoryRepository {
        val categories = MutableStateFlow(initial)
        val renamedIds = mutableListOf<String>()
        var deleteCalls = 0
        var deleteOutcome: CategoryDeleteOutcome = CategoryDeleteOutcome.Deleted

        override fun observeCategories(): Flow<List<CategoryEntity>> = categories

        override suspend fun createCategory(name: String): Result<CategoryEntity> = error("Not used")

        override suspend fun archiveCustomCategory(id: String): Result<Unit> = result {
            val target = categories.value.single { it.id == id }
            check(!target.isBuiltIn)
            categories.value = categories.value.map { if (it.id == id) it.copy(isArchived = true) else it }
        }

        override suspend fun renameCustomCategory(id: String, name: String): Result<Unit> = result {
            val normalized = name.trim().lowercase()
            val target = categories.value.single { it.id == id }
            require(normalized.isNotBlank())
            check(!target.isBuiltIn && categories.value.none { it.id != id && it.normalizedName == normalized })
            renamedIds += id
            categories.value = categories.value.map {
                if (it.id == id) it.copy(name = name.trim(), normalizedName = normalized) else it
            }
        }

        override suspend fun deleteCustomCategory(id: String): Result<CategoryDeleteOutcome> = result {
            deleteCalls += 1
            val target = categories.value.single { it.id == id }
            check(!target.isBuiltIn)
            if (deleteOutcome == CategoryDeleteOutcome.Deleted) {
                categories.value = categories.value.filterNot { it.id == id }
            }
            deleteOutcome
        }

        private fun <T> result(block: () -> T): Result<T> = runCatching(block)
    }
}
