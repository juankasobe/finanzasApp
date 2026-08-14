package com.saldoclaro.finance.feature.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.core.designsystem.RetryableErrorState
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import java.time.LocalDate
@Composable
fun TransactionScreen(viewModel: TransactionViewModel, categories: List<CategoryEntity>) {
    val state by viewModel.state.collectAsState()
    var type by remember { mutableStateOf<TransactionType?>(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            Button(onClick = { typeMenuOpen = true }) { Text(type?.name ?: "Choose type") }
            DropdownMenu(typeMenuOpen, { typeMenuOpen = false }) {
                TransactionType.entries.forEach { option ->
                    DropdownMenuItem({ Text(option.name) }, { type = option; typeMenuOpen = false })
                }
            }
        }
        OutlinedTextField(amount, { amount = it }, label = { Text("Amount") })
        Box {
            Button(onClick = { categoryMenuOpen = true }, enabled = categories.isNotEmpty()) {
                Text(category?.name ?: if (categories.isEmpty()) "No active categories" else "Choose category")
            }
            DropdownMenu(categoryMenuOpen, { categoryMenuOpen = false }) {
                categories.forEach { option ->
                    DropdownMenuItem({ Text(option.name) }, { category = option; categoryMenuOpen = false })
                }
            }
        }
        OutlinedTextField(date, { date = it }, label = { Text("Date (YYYY-MM-DD)") })
        Button(onClick = {
            viewModel.save(TransactionDraft(type, amount, category?.id, runCatching { LocalDate.parse(date) }.getOrNull()))
        }) { Text("Add transaction") }
        when (val current = state) {
            TransactionUiState.Empty -> Text("No transactions yet")
            is TransactionUiState.Content -> Ledger(current.transactions, viewModel::requestDelete)
            is TransactionUiState.Validation -> {
                Ledger(current.transactions, viewModel::requestDelete)
                Text("Complete the required transaction fields")
            }
            is TransactionUiState.ConfirmDelete -> {
                Text("Delete this transaction?")
                Button(onClick = viewModel::confirmDelete) { Text("Delete") }
                Button(onClick = viewModel::cancelDelete) { Text("Cancel") }
            }
            is TransactionUiState.Error -> {
                Ledger(current.transactions, viewModel::requestDelete)
                RetryableErrorState(
                    message = current.message,
                    canRetry = current.canRetry,
                    onRetry = viewModel::retry,
                )
            }
        }
    }
}

@Composable
private fun Ledger(transactions: List<Transaction>, onDelete: (String) -> Unit) {
    transactions.forEach { transaction ->
        ListItem(
            headlineContent = { Text("${transaction.type}: ${transaction.amountCents} cents") },
            supportingContent = { Text("${transaction.categoryId} - ${transaction.localDate}") },
            trailingContent = { Button(onClick = { onDelete(transaction.id) }) { Text("Delete") } },
        )
    }
}
