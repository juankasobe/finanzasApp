package com.saldoclaro.finance.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saldoclaro.finance.core.designsystem.FinanceCard
import com.saldoclaro.finance.core.designsystem.FinanceEmptyState
import com.saldoclaro.finance.core.designsystem.FinanceExpense
import com.saldoclaro.finance.core.designsystem.FinanceIncome
import com.saldoclaro.finance.core.designsystem.FinanceScreenHeader
import com.saldoclaro.finance.core.designsystem.FinanceTransactionRow
import com.saldoclaro.finance.core.designsystem.RetryableErrorState
import com.saldoclaro.finance.core.designsystem.categoryPresentationName
import com.saldoclaro.finance.core.designsystem.formatCents
import com.saldoclaro.finance.core.designsystem.formatDate
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionDraft
import com.saldoclaro.finance.domain.model.TransactionType
import java.time.LocalDate

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    categories: List<CategoryEntity>,
    entryRequest: Int = 0,
) {
    val state by viewModel.state.collectAsState()
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(entryRequest) {
        if (entryRequest > 0) scrollState.animateScrollTo(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinanceScreenHeader(
            title = "Transactions",
            subtitle = "Record and review your monthly activity",
        )
        TransactionEditor(
            type = type,
            onTypeChange = { type = it },
            amount = amount,
            onAmountChange = { amount = it },
            category = category,
            onCategoryChange = { category = it },
            date = date,
            onDateChange = { date = it },
            categories = categories,
            categoryMenuOpen = categoryMenuOpen,
            onCategoryMenuChange = { categoryMenuOpen = it },
            onSave = {
                viewModel.save(
                    TransactionDraft(
                        type = type,
                        amount = amount,
                        categoryId = category?.id,
                        localDate = runCatching { LocalDate.parse(date) }.getOrNull(),
                    ),
                )
            },
        )
        when (val current = state) {
            TransactionUiState.Empty -> TransactionActivity(emptyList(), viewModel::requestDelete)
            is TransactionUiState.Content -> TransactionActivity(current.transactions, viewModel::requestDelete)
            is TransactionUiState.Validation -> {
                ValidationMessage()
                TransactionActivity(current.transactions, viewModel::requestDelete)
            }
            is TransactionUiState.ConfirmDelete -> DeleteConfirmation(
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::cancelDelete,
            )
            is TransactionUiState.Error -> {
                TransactionActivity(current.transactions, viewModel::requestDelete)
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
private fun TransactionEditor(
    type: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    category: CategoryEntity?,
    onCategoryChange: (CategoryEntity) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    categories: List<CategoryEntity>,
    categoryMenuOpen: Boolean,
    onCategoryMenuChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    FinanceCard {
        Text(text = "New transaction", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Choose a type, amount, category, and date.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = "Type", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TransactionType.entries.forEach { option ->
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = type == option,
                    onClick = { onTypeChange(option) },
                    label = { Text(option.presentationName()) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (option == TransactionType.INCOME) Icons.Outlined.Add else Icons.Outlined.Remove,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        Text(text = "Category", style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCategoryMenuChange(true) },
                enabled = categories.isNotEmpty(),
            ) {
                Text(
                    text = category?.name ?: if (categories.isEmpty()) "No active categories" else "Choose category",
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Select category")
            }
            DropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { onCategoryMenuChange(false) },
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onCategoryChange(option)
                            onCategoryMenuChange(false)
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Amount") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = date,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Date (YYYY-MM-DD)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = onSave) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Add transaction")
        }
    }
}

@Composable
private fun TransactionActivity(transactions: List<Transaction>, onDelete: (String) -> Unit) {
    if (transactions.isEmpty()) {
        FinanceEmptyState(
            icon = Icons.Outlined.ReceiptLong,
            title = "No transactions yet",
            message = "Your activity will appear here after you add a transaction.",
        )
        return
    }

    Text(text = "Activity", style = MaterialTheme.typography.titleMedium)
    FinanceCard(
        contentPadding = 0.dp,
        verticalArrangement = Arrangement.Top,
    ) {
        transactions.forEachIndexed { index, transaction ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            FinanceTransactionRow(
                categoryKey = transaction.categoryId,
                title = transaction.type.presentationName(),
                subtitle = "${categoryPresentationName(transaction.categoryId)} - ${formatDate(transaction.localDate)}",
                amount = transaction.presentationAmount(),
                amountColor = if (transaction.type == TransactionType.INCOME) FinanceIncome else FinanceExpense,
                onDelete = { onDelete(transaction.id) },
                deleteContentDescription = "Delete ${transaction.type.presentationName().lowercase()} transaction",
            )
        }
    }
}

@Composable
private fun ValidationMessage() {
    FinanceCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = "Complete the required transaction fields",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun DeleteConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this transaction?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun TransactionType.presentationName(): String = when (this) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
}

private fun Transaction.presentationAmount(): String = when (type) {
    TransactionType.INCOME -> "+${formatCents(amountCents)}"
    TransactionType.EXPENSE -> "-${formatCents(amountCents)}"
}
