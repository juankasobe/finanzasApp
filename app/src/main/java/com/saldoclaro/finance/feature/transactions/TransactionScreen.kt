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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.saldoclaro.finance.R
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
import java.util.Locale

@Composable
fun TransactionScreen(
    viewModel: TransactionViewModel,
    categories: List<CategoryEntity>,
    categoryMetadata: List<CategoryEntity> = categories,
    entryRequest: Int = 0,
) {
    val state by viewModel.state.collectAsState()
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val categoryNames = categoryMetadata.associate { it.id to it.name }
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
            title = stringResource(R.string.transaction_screen_title),
            subtitle = stringResource(R.string.transaction_screen_subtitle),
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
            TransactionUiState.Empty -> TransactionActivity(emptyList(), viewModel::requestDelete, categoryNames)
            is TransactionUiState.Content -> TransactionActivity(current.transactions, viewModel::requestDelete, categoryNames)
            is TransactionUiState.Validation -> {
                ValidationMessage()
                TransactionActivity(current.transactions, viewModel::requestDelete, categoryNames)
            }
            is TransactionUiState.ConfirmDelete -> DeleteConfirmation(
                onConfirm = viewModel::confirmDelete,
                onDismiss = viewModel::cancelDelete,
            )
            is TransactionUiState.Error -> {
                TransactionActivity(current.transactions, viewModel::requestDelete, categoryNames)
                RetryableErrorState(
                    reason = current.reason,
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
        Text(text = stringResource(R.string.transaction_new_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.transaction_new_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = stringResource(R.string.transaction_type_label), style = MaterialTheme.typography.labelLarge)
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
        Text(text = stringResource(R.string.category_label), style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCategoryMenuChange(true) },
                enabled = categories.isNotEmpty(),
            ) {
                Text(
                    text = category?.let { categoryPresentationName(it.id, it.name) }
                        ?: if (categories.isEmpty()) stringResource(R.string.no_active_categories)
                        else stringResource(R.string.choose_category),
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = stringResource(R.string.action_select_category))
            }
            DropdownMenu(
                expanded = categoryMenuOpen,
                onDismissRequest = { onCategoryMenuChange(false) },
            ) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(categoryPresentationName(option.id, option.name)) },
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
            label = { Text(stringResource(R.string.transaction_amount_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = date,
            onValueChange = onDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.transaction_date_label)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = onSave) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.transaction_add))
        }
    }
}

@Composable
private fun TransactionActivity(
    transactions: List<Transaction>,
    onDelete: (String) -> Unit,
    categoryNames: Map<String, String>,
) {
    if (transactions.isEmpty()) {
        FinanceEmptyState(
            icon = Icons.Outlined.ReceiptLong,
            title = stringResource(R.string.transaction_empty_title),
            message = stringResource(R.string.transaction_empty_message),
        )
        return
    }

    Text(text = stringResource(R.string.transaction_activity), style = MaterialTheme.typography.titleMedium)
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
                subtitle = "${categoryPresentationName(transaction.categoryId, categoryNames[transaction.categoryId])} - ${formatDate(transaction.localDate)}",
                amount = transaction.presentationAmount(),
                amountColor = if (transaction.type == TransactionType.INCOME) FinanceIncome else FinanceExpense,
                onDelete = { onDelete(transaction.id) },
                deleteContentDescription = stringResource(
                    R.string.transaction_delete_description,
                    transaction.type.presentationName().lowercase(Locale.ROOT),
                ),
            )
        }
    }
}

@Composable
private fun ValidationMessage() {
    FinanceCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = stringResource(R.string.transaction_validation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun DeleteConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transaction_delete_title)) },
        text = { Text(stringResource(R.string.transaction_delete_message)) },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun TransactionType.presentationName(): String = when (this) {
    TransactionType.INCOME -> stringResource(R.string.transaction_type_income)
    TransactionType.EXPENSE -> stringResource(R.string.transaction_type_expense)
}

private fun Transaction.presentationAmount(): String = when (type) {
    TransactionType.INCOME -> "+${formatCents(amountCents)}"
    TransactionType.EXPENSE -> "-${formatCents(amountCents)}"
}
