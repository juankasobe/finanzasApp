package com.saldoclaro.finance.feature.budgets

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
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.saldoclaro.finance.core.designsystem.CategoryIconChip
import com.saldoclaro.finance.core.designsystem.FinanceCard
import com.saldoclaro.finance.core.designsystem.FinanceEmptyState
import com.saldoclaro.finance.core.designsystem.FinanceExpense
import com.saldoclaro.finance.core.designsystem.FinanceIncome
import com.saldoclaro.finance.core.designsystem.FinanceProgressBar
import com.saldoclaro.finance.core.designsystem.FinanceScreenHeader
import com.saldoclaro.finance.core.designsystem.FinanceStatusPill
import com.saldoclaro.finance.core.designsystem.FinanceTextMuted
import com.saldoclaro.finance.core.designsystem.FinanceWarning
import com.saldoclaro.finance.core.designsystem.RetryableErrorState
import com.saldoclaro.finance.core.designsystem.categoryPresentationName
import com.saldoclaro.finance.core.designsystem.formatCents
import com.saldoclaro.finance.data.local.CategoryEntity
import com.saldoclaro.finance.domain.usecase.BudgetProgressItem
import com.saldoclaro.finance.domain.usecase.BudgetState

@Composable
fun BudgetScreen(viewModel: BudgetViewModel, categories: List<CategoryEntity>) {
    val state by viewModel.state.collectAsState()
    val activeCategories = categories.filterNot { it.isArchived }
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var limit by remember { mutableStateOf("") }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FinanceScreenHeader(
            title = stringResource(R.string.budget_screen_title),
            subtitle = stringResource(R.string.budget_screen_subtitle),
        )
        BudgetEditor(
            category = category,
            onCategoryChange = { category = it },
            limit = limit,
            onLimitChange = { limit = it },
            categories = activeCategories,
            categoryMenuOpen = categoryMenuOpen,
            onCategoryMenuChange = { categoryMenuOpen = it },
            onSave = { category?.let { viewModel.saveLimit(it.id, limit) } },
        )
        when (val current = state) {
            is BudgetUiState.Content -> BudgetProgress(current.progress, categories)
            is BudgetUiState.Validation -> ValidationMessage()
            is BudgetUiState.Error -> RetryableErrorState(current.reason, current.canRetry, viewModel::retry)
        }
    }
}

@Composable
private fun BudgetEditor(
    category: CategoryEntity?,
    onCategoryChange: (CategoryEntity) -> Unit,
    limit: String,
    onLimitChange: (String) -> Unit,
    categories: List<CategoryEntity>,
    categoryMenuOpen: Boolean,
    onCategoryMenuChange: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    FinanceCard {
        Text(text = stringResource(R.string.budget_limit_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.budget_limit_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            value = limit,
            onValueChange = onLimitChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.budget_limit_title)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Button(modifier = Modifier.fillMaxWidth(), onClick = onSave) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.budget_save_limit))
        }
    }
}

@Composable
private fun BudgetProgress(progress: List<BudgetProgressItem>, categories: List<CategoryEntity>) {
    Text(text = stringResource(R.string.budget_overview), style = MaterialTheme.typography.titleMedium)
    if (progress.isEmpty()) {
        FinanceEmptyState(
            icon = Icons.Outlined.Insights,
            title = stringResource(R.string.budget_no_activity_title),
            message = stringResource(R.string.budget_no_activity_message),
        )
        return
    }

    BudgetSummary(progress)
    Text(text = stringResource(R.string.budget_category_budgets), style = MaterialTheme.typography.titleMedium)
    val categoryNames = categories.associate { it.id to it.name }
    progress.forEach { item ->
        BudgetProgressCard(
            item = item,
            categoryName = categoryPresentationName(item.categoryId, categoryNames[item.categoryId]),
        )
    }
}

@Composable
private fun BudgetSummary(progress: List<BudgetProgressItem>) {
    val limitedProgress = progress.filter { it.limitCents != null }
    if (limitedProgress.isEmpty()) {
        FinanceCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
            Text(text = stringResource(R.string.budget_no_limits_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.budget_no_limits_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val totalLimit = limitedProgress.sumOf { it.limitCents ?: 0L }
    val totalSpent = limitedProgress.sumOf { it.spentCents }
    val percentage = ((totalSpent.toDouble() / totalLimit) * 100).toInt()
    val remaining = totalLimit - totalSpent
    val statusColor = if (remaining < 0L) FinanceExpense else FinanceIncome
    FinanceCard(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = stringResource(R.string.budget_total_planned_spending),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = formatCents(totalSpent), style = MaterialTheme.typography.headlineSmall)
            Text(
                text = stringResource(R.string.budget_of, formatCents(totalLimit)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FinanceProgressBar(
            fraction = totalSpent.toFloat() / totalLimit.toFloat(),
            color = statusColor,
            description = stringResource(R.string.budget_progress_description, percentage),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.budget_percentage_used, percentage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (remaining >= 0L) {
                    stringResource(R.string.dashboard_amount_left, formatCents(remaining))
                } else {
                    stringResource(R.string.dashboard_amount_over, formatCents(-remaining))
                },
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun BudgetProgressCard(item: BudgetProgressItem, categoryName: String) {
    val status = item.statusPresentation()
    FinanceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconChip(categoryKey = categoryName, size = 36.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = categoryName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (item.limitCents == null) stringResource(R.string.budget_no_monthly_limit)
                    else stringResource(R.string.budget_monthly_limit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FinanceStatusPill(text = status.label, color = status.color)
        }
        if (item.limitCents == null) {
            Text(
                text = stringResource(R.string.budget_spent, formatCents(item.spentCents)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val percentage = ((item.spentCents.toDouble() / item.limitCents) * 100).toInt()
            FinanceProgressBar(
                fraction = item.spentCents.toFloat() / item.limitCents.toFloat(),
                color = status.color,
                description = stringResource(R.string.dashboard_progress_description, categoryName, percentage),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(
                        R.string.dashboard_amount_of,
                        formatCents(item.spentCents),
                        formatCents(item.limitCents),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.remainingCents?.let { remainingLabel(it) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = status.color,
                )
            }
        }
    }
}

@Composable
private fun ValidationMessage() {
    FinanceCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
        Text(
            text = stringResource(R.string.error_invalid_amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun BudgetProgressItem.statusPresentation(): BudgetStatus = when (state) {
    BudgetState.UNDER -> BudgetStatus(stringResource(R.string.dashboard_status_under), FinanceIncome)
    BudgetState.AT_LIMIT -> BudgetStatus(stringResource(R.string.dashboard_status_at_limit), FinanceWarning)
    BudgetState.OVER -> BudgetStatus(stringResource(R.string.dashboard_status_over), FinanceExpense)
    BudgetState.NO_BUDGET -> BudgetStatus(stringResource(R.string.dashboard_status_no_limit), FinanceTextMuted)
}

@Composable
private fun remainingLabel(remainingCents: Long): String = when {
    remainingCents > 0L -> stringResource(R.string.dashboard_amount_left, formatCents(remainingCents))
    remainingCents == 0L -> stringResource(R.string.dashboard_limit_reached)
    else -> stringResource(R.string.dashboard_amount_over, formatCents(-remainingCents))
}

private data class BudgetStatus(val label: String, val color: androidx.compose.ui.graphics.Color)
