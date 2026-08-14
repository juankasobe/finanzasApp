package com.saldoclaro.finance.feature.transactions

import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionViewModelTest {
    private val month = YearMonth.of(2026, 8)
    private val date = LocalDate.of(2026, 8, 12)

    @Test
    fun `valid transaction reopens with its persisted fields`() = runBlocking {
        val repository = FakeTransactionRepository()
        viewModel(repository).save(validDraft())

        val saved = content(viewModel(repository).state.value).single()

        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(4_200L, saved.amountCents)
        assertEquals("groceries", saved.categoryId)
        assertEquals(date, saved.localDate)
    }

    @Test
    fun `zero fractional cent and incomplete drafts show field validation without creating records`() = runBlocking {
        val repository = FakeTransactionRepository()
        val viewModel = viewModel(repository)

        viewModel.save(validDraft(amount = "0.00"))
        assertEquals(setOf(TransactionField.AMOUNT), validation(viewModel.state.value).invalidFields)

        viewModel.save(validDraft(amount = "12.345"))
        assertEquals(setOf(TransactionField.AMOUNT), validation(viewModel.state.value).invalidFields)

        viewModel.save(validDraft(categoryId = null))
        assertEquals(setOf(TransactionField.CATEGORY), validation(viewModel.state.value).invalidFields)
        assertTrue(viewModel(repository).state.value is TransactionUiState.Empty)
    }

    @Test
    fun `only confirmed deletion removes the selected transaction`() = runBlocking {
        val existing = transaction(id = "purchase", amountCents = 2_500L)
        val viewModel = viewModel(FakeTransactionRepository(listOf(existing)))

        viewModel.requestDelete(existing.id)
        assertEquals(existing, confirmation(viewModel.state.value).transaction)

        viewModel.confirmDelete()
        assertTrue(viewModel.state.value is TransactionUiState.Empty)
    }

    @Test
    fun `canceled deletion keeps existing activity unchanged`() = runBlocking {
        val existing = transaction(id = "purchase", amountCents = 2_500L)
        val viewModel = viewModel(FakeTransactionRepository(listOf(existing)))

        viewModel.requestDelete(existing.id)
        viewModel.cancelDelete()

        assertEquals(listOf(existing), content(viewModel.state.value))
    }

    @Test
    fun `an empty ledger exposes an explicit empty state`() = runBlocking {
        val state = viewModel(FakeTransactionRepository()).state.value

        assertTrue(state is TransactionUiState.Empty)
    }

    @Test
    fun `storage failure is recoverable and preserves existing activity`() = runBlocking {
        val existing = transaction(id = "rent", amountCents = 90_000L)
        val repository = FakeTransactionRepository(listOf(existing)).apply {
            nextSaveFailure = IllegalStateException("storage unavailable")
        }
        val viewModel = viewModel(repository)

        viewModel.save(validDraft())

        val error = error(viewModel.state.value)
        assertTrue(error.canRetry)
        assertEquals(listOf(existing), error.transactions)
    }

    private fun viewModel(repository: TransactionRepository) =
        TransactionViewModel(repository, month, Dispatchers.Unconfined)

    private fun validDraft(
        amount: String = "42.00",
        categoryId: String? = "groceries",
    ) = TransactionDraft(TransactionType.EXPENSE, amount, categoryId, date)

    private fun transaction(id: String, amountCents: Long) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amountCents = amountCents,
        categoryId = "groceries",
        localDate = date,
    )

    private fun content(state: TransactionUiState): List<Transaction> {
        assertTrue("Expected ledger content but was $state", state is TransactionUiState.Content)
        return (state as TransactionUiState.Content).transactions
    }

    private fun validation(state: TransactionUiState): TransactionUiState.Validation {
        assertTrue("Expected validation state but was $state", state is TransactionUiState.Validation)
        return state as TransactionUiState.Validation
    }

    private fun confirmation(state: TransactionUiState): TransactionUiState.ConfirmDelete {
        assertTrue("Expected delete confirmation but was $state", state is TransactionUiState.ConfirmDelete)
        return state as TransactionUiState.ConfirmDelete
    }

    private fun error(state: TransactionUiState): TransactionUiState.Error {
        assertTrue("Expected recoverable error but was $state", state is TransactionUiState.Error)
        return state as TransactionUiState.Error
    }

    private class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {
        private val transactions = MutableStateFlow(initial)
        var nextSaveFailure: Throwable? = null

        override fun observeMonth(month: YearMonth): Flow<List<Transaction>> =
            transactions.map { records -> records.filter { YearMonth.from(it.localDate) == month } }

        override suspend fun save(draft: TransactionDraft): Result<Transaction> {
            nextSaveFailure?.let { return Result.failure(it) }
            val saved = Transaction(
                id = "generated-${transactions.value.size + 1}",
                type = draft.type!!,
                amountCents = draft.amount.toBigDecimal().movePointRight(2).longValueExact(),
                categoryId = draft.categoryId!!,
                localDate = draft.localDate!!,
            )
            transactions.value += saved
            return Result.success(saved)
        }

        override suspend fun delete(id: String): Result<Unit> {
            transactions.value = transactions.value.filterNot { it.id == id }
            return Result.success(Unit)
        }
    }
}
