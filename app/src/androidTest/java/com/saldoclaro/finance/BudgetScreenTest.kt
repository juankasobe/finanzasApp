package com.saldoclaro.finance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saldoclaro.finance.core.designsystem.SaldoClaroTheme
import com.saldoclaro.finance.core.time.CurrentMonthSource
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.domain.model.*
import com.saldoclaro.finance.domain.repository.*
import com.saldoclaro.finance.feature.budgets.BudgetScreen
import com.saldoclaro.finance.feature.budgets.BudgetViewModel
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<DashboardTestActivity>()
    private val month = YearMonth.of(2026, 3)
    private val clock = Clock.fixed(Instant.parse("2026-04-01T00:30:00Z"), ZoneId.of("America/Los_Angeles"))

    @Test
    fun limitCardOpensExactTargetAndValidEditChangesOnlyItsAmount() {
        val next = month.plusMonths(1)
        val repository = render(budgets = listOf(budget(), budget(next, 7_000L)))
        openManagement()
        composeTestRule.onNodeWithText("Gestionar límite mensual").assertExists()
        composeTestRule.onNodeWithText("Supermercado · marzo de 2026").assertExists()
        composeTestRule.onNodeWithText("50.00").performTextReplacement("60.00")
        composeTestRule.onNodeWithText("Guardar cambios").performClick()
        composeTestRule.waitUntil(5_000) { repository.editCalls == 1 }
        Assert.assertEquals(6_000L, repository.budgets.value.first { it.month == month }.limitCents)
        Assert.assertEquals(7_000L, repository.budgets.value.first { it.month == next }.limitCents)
    }

    @Test
    fun invalidEditAndDeleteCancellationPreserveDataThenConfirmedDeleteKeepsSpending() {
        val repository = render(withSpending = true)
        openManagement()
        composeTestRule.onNodeWithText("50.00").performTextReplacement("0")
        composeTestRule.onNodeWithText("Guardar cambios").performClick()
        composeTestRule.onNodeWithText("Ingresa un importe positivo con hasta dos decimales.").assertExists()
        Assert.assertEquals(0, repository.editCalls)
        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.onNodeWithText("25,00\u00a0US$ de 50,00\u00a0US$", useUnmergedTree = true).assertExists()
        openManagement()
        composeTestRule.onNodeWithText("Eliminar límite").performClick()
        composeTestRule.onNodeWithText("¿Eliminar este límite mensual?").assertExists()
        composeTestRule.onNodeWithText("Se eliminará el límite de Supermercado para marzo de 2026. Tus transacciones y otros meses se conservarán.").assertExists()
        composeTestRule.onNodeWithText("Cancelar").performClick()
        Assert.assertEquals(0, repository.deleteCalls)
        openManagement()
        composeTestRule.onNodeWithText("Eliminar límite").performClick()
        composeTestRule.onNodeWithText("Eliminar").performClick()
        composeTestRule.waitUntil(5_000) { repository.deleteCalls == 1 }
        composeTestRule.onNodeWithText("Sin límite mensual").assertExists()
        composeTestRule.onNodeWithText("Gastado 25,00\u00a0US$").assertExists()
    }

    @Test
    fun invalidEditKeepsManagementContextAvailableForCorrection() {
        val repository = render()
        openManagement()
        composeTestRule.onNodeWithText("50.00").performTextReplacement("0")
        composeTestRule.onNodeWithText("Guardar cambios").performClick()
        composeTestRule.onNodeWithText("Ingresa un importe positivo con hasta dos decimales.").assertExists()
        composeTestRule.onNodeWithText("Importe").assertExists()
        composeTestRule.onNodeWithText("0").performTextReplacement("60.00")
        composeTestRule.onNodeWithText("Guardar cambios").performClick()
        composeTestRule.waitUntil(5_000) { repository.editCalls == 1 }
        composeTestRule.onNodeWithText("Gestionar límite mensual").assertDoesNotExist()
    }

    @Test
    fun archivedLimitOffersDeleteWithoutEditAffordance() {
        render(archived = true)
        openManagement()
        composeTestRule.onNodeWithText("Esta categoría está archivada. Solo puedes eliminar su límite.").assertExists()
        composeTestRule.onNodeWithText("Guardar cambios").assertDoesNotExist()
        composeTestRule.onNodeWithText("Importe").assertDoesNotExist()
        composeTestRule.onNodeWithText("Eliminar límite").assertExists()
    }

    @Test
    fun spendingWithoutLimitRemainsVisibleWithoutManagementAction() {
        render(withSpending = true, budgets = emptyList())
        composeTestRule.onNodeWithText("Sin límite mensual").assertExists()
        composeTestRule.onNodeWithText("Gastado 25,00\u00a0US$").assertExists()
        composeTestRule.onNodeWithContentDescription("Gestionar límite de Supermercado de marzo de 2026").assertDoesNotExist()
    }

    private fun openManagement() = composeTestRule
        .onNodeWithContentDescription("Gestionar límite de Supermercado de marzo de 2026")
        .performClick()

    private fun render(
        archived: Boolean = false,
        withSpending: Boolean = false,
        budgets: List<Budget> = listOf(budget()),
    ): FakeBudgetRepository {
        val repository = FakeBudgetRepository(budgets)
        val viewModel = BudgetViewModel(
            FakeTransactionRepository(if (withSpending) listOf(transaction()) else emptyList()), repository, clock, clock.zone,
            object : CurrentMonthSource {
                override val month = MutableStateFlow(this@BudgetScreenTest.month)
                override fun setForeground(active: Boolean) = Unit
                override fun refresh() = Unit
            }, Dispatchers.Main.immediate,
        )
        composeTestRule.setContent { SaldoClaroTheme { BudgetScreen(viewModel, listOf(CategoryEntity("groceries", "Groceries", "groceries", true, archived))) } }
        composeTestRule.waitForIdle()
        return repository
    }

    private fun budget(month: YearMonth = this.month, limitCents: Long = 5_000L) = Budget("groceries", month, limitCents)
    private fun transaction() = Transaction("expense", TransactionType.EXPENSE, 2_500L, "groceries", LocalDate.of(2026, 3, 15))

    private class FakeTransactionRepository(private val values: List<Transaction>) : TransactionRepository {
        override fun observeMonth(month: YearMonth) = flowOf(values.filter { YearMonth.from(it.localDate) == month })
        override suspend fun save(draft: TransactionDraft) = error("unused")
        override suspend fun delete(id: String) = error("unused")
    }

    private class FakeBudgetRepository(initial: List<Budget>) : BudgetRepository {
        val budgets = MutableStateFlow(initial)
        var editCalls = 0
        var deleteCalls = 0
        override fun observeMonth(month: YearMonth) = budgets.map { it.filter { budget -> budget.month == month } }
        override suspend fun save(categoryId: String, month: YearMonth, limitCents: Long) = Result.success(Unit)
        override suspend fun editAmount(target: BudgetTarget, newLimitCents: Long): Result<Unit> {
            editCalls++
            budgets.value = budgets.value.map { if (it.categoryId == target.categoryId && it.month == target.month) it.copy(limitCents = newLimitCents) else it }
            return Result.success(Unit)
        }
        override suspend fun delete(target: BudgetTarget): Result<DeleteEvidence> {
            deleteCalls++
            budgets.value = budgets.value.filterNot { it.categoryId == target.categoryId && it.month == target.month }
            return Result.success(DeleteEvidence(1))
        }
    }
}
