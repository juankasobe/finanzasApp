package com.saldoclaro.finance

import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saldoclaro.finance.core.designsystem.SaldoClaroTheme
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.data.repository.CategoryDeleteOutcome
import com.saldoclaro.finance.data.repository.CategoryRepository
import com.saldoclaro.finance.feature.categories.CategoryScreen
import com.saldoclaro.finance.feature.categories.CategoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val longName = "Una categoría personalizada con un nombre extraordinariamente largo"
    private val builtIn = category("builtin-groceries", "Groceries", builtIn = true)
    private val active = category("custom-long", longName)
    private val archived = category("custom-old", "Old category", archived = true)

    @Test
    fun cardsHaveUniformBoundedHeightAndLongTitleIsConstrained() {
        render()

        listOf(builtIn, active, archived).forEach {
            composeTestRule.onNodeWithTag("category-card-${it.id}").performScrollTo().assertHeightIsEqualTo(152.dp)
        }
        val title = composeTestRule.onNodeWithTag("category-title-${active.id}").assertTextEquals(longName)
        assertTrue(title.fetchSemanticsNode().boundsInRoot.height <= with(composeTestRule.density) { 48.dp.toPx() })
    }

    @Test
    fun menusExposeOnlyAllowedActions() {
        render()

        composeTestRule.onNodeWithContentDescription("Gestionar Supermercado").assertDoesNotExist()
        openMenu(active)
        listOf("Editar", "Eliminar", "Archivar").forEach { composeTestRule.onNodeWithText(it).assertExists() }
        composeTestRule.onNodeWithText("Editar").performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        openMenu(archived)
        composeTestRule.onNodeWithText("Editar").assertExists()
        composeTestRule.onNodeWithText("Eliminar").assertExists()
        composeTestRule.onNodeWithText("Archivar").assertDoesNotExist()
    }

    @Test
    fun renameIsPrefilledAndDeleteRequiresConfirmation() {
        val repository = render()
        openMenu(active)
        composeTestRule.onNodeWithText("Editar").performClick()
        composeTestRule.onNodeWithTag("category-rename-input").assertTextContains(longName)
            .performTextReplacement("Viajes")
        composeTestRule.onNodeWithText("Guardar").performClick()
        composeTestRule.waitUntil { repository.renamed == active.id to "Viajes" }

        openMenu(active.copy(name = "Viajes"))
        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.onNodeWithText("¿Eliminar esta categoría?").assertExists()
        composeTestRule.onNodeWithText("Cancelar").performClick()
    }

    @Test
    fun referencedDeleteOffersArchiveAndSuccessDismissesDialog() {
        val repository = render().apply { deleteOutcome = CategoryDeleteOutcome.InUse }
        openMenu(active)
        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.onNodeWithText("Eliminar", useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithText(
            "No se puede eliminar porque esta categoría forma parte de tu historial financiero.",
        ).assertExists()
        composeTestRule.onNodeWithText("Archivar").performClick()
        composeTestRule.waitUntil { active.id in repository.archivedIds }
        composeTestRule.onNodeWithText(
            "No se puede eliminar porque esta categoría forma parte de tu historial financiero.",
        ).assertDoesNotExist()
    }

    private fun openMenu(category: CategoryEntity) {
        composeTestRule.onNodeWithTag("category-card-${category.id}").performScrollTo()
        composeTestRule.onNodeWithContentDescription("Gestionar ${category.name}").performClick()
    }

    private fun render(): FakeCategoryRepository {
        val repository = FakeCategoryRepository(listOf(builtIn, active, archived))
        val viewModel = CategoryViewModel(repository, Dispatchers.Main.immediate)
        composeTestRule.activity.setContent { SaldoClaroTheme { CategoryScreen(viewModel) } }
        composeTestRule.waitForIdle()
        return repository
    }

    private fun category(id: String, name: String, builtIn: Boolean = false, archived: Boolean = false) =
        CategoryEntity(id, name, name.lowercase(), builtIn, archived)

    private class FakeCategoryRepository(initial: List<CategoryEntity>) : CategoryRepository {
        private val categories = MutableStateFlow(initial)
        var renamed: Pair<String, String>? = null
        var deleteOutcome = CategoryDeleteOutcome.Deleted
        val archivedIds = mutableListOf<String>()

        override fun observeCategories() = categories
        override suspend fun createCategory(name: String) = error("unused")
        override suspend fun renameCustomCategory(id: String, name: String): Result<Unit> {
            renamed = id to name
            categories.value = categories.value.map { if (it.id == id) it.copy(name = name) else it }
            return Result.success(Unit)
        }
        override suspend fun archiveCustomCategory(id: String): Result<Unit> {
            archivedIds += id
            categories.value = categories.value.map { if (it.id == id) it.copy(isArchived = true) else it }
            return Result.success(Unit)
        }
        override suspend fun deleteCustomCategory(id: String) = Result.success(deleteOutcome)
    }
}
