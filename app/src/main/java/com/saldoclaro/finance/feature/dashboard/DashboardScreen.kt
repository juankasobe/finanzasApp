package com.saldoclaro.finance.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saldoclaro.finance.core.designsystem.FinanceCard
import com.saldoclaro.finance.core.designsystem.FinanceEmptyState
import com.saldoclaro.finance.core.designsystem.FinanceExpense
import com.saldoclaro.finance.core.designsystem.FinanceIncome
import com.saldoclaro.finance.core.designsystem.FinanceProgressBar
import com.saldoclaro.finance.core.designsystem.FinanceScreenHeader
import com.saldoclaro.finance.core.designsystem.FinanceStatusPill
import com.saldoclaro.finance.core.designsystem.FinanceTextMuted
import com.saldoclaro.finance.core.designsystem.FinanceTransactionRow
import com.saldoclaro.finance.core.designsystem.FinanceWarning
import com.saldoclaro.finance.core.designsystem.RetryableErrorState
import com.saldoclaro.finance.core.designsystem.categoryPresentationName
import com.saldoclaro.finance.core.designsystem.formatCents
import com.saldoclaro.finance.core.designsystem.formatDate
import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction
import com.saldoclaro.finance.domain.model.TransactionType
import com.saldoclaro.finance.domain.usecase.BudgetProgressItem
import com.saldoclaro.finance.domain.usecase.BudgetState

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (val current = state) {
            DashboardUiState.Loading -> DashboardLoading()
            is DashboardUiState.Content -> DashboardContent(
                totals = current.totals,
                transactions = current.recentActivity,
                budgetOverview = current.budgetOverview,
            )
            is DashboardUiState.Error -> RetryableErrorState(
                message = current.message,
                canRetry = current.canRetry,
                onRetry = viewModel::retry,
            )
        }
    }
}

@Composable
private fun DashboardLoading() {
    FinanceCard(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
        Text(text = "Loading dashboard", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Preparing your current month overview.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DashboardContent(
    totals: MonthTotals,
    transactions: List<Transaction>,
    budgetOverview: DashboardBudgetOverview,
) {
    FinanceScreenHeader(
        title = "Dashboard",
        subtitle = "Your current month at a glance",
    )
    BalanceCard(totals)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DashboardMetric(
            label = "Income",
            amountCents = totals.incomeCents,
            icon = Icons.Outlined.Add,
            accent = FinanceIncome,
            modifier = Modifier.weight(1f),
        )
        DashboardMetric(
            label = "Expenses",
            amountCents = totals.expenseCents,
            icon = Icons.Outlined.Remove,
            accent = FinanceExpense,
            modifier = Modifier.weight(1f),
        )
    }
    Text(text = "Budget overview", style = MaterialTheme.typography.titleMedium)
    when (budgetOverview) {
        DashboardBudgetOverview.NoBudgets -> FinanceEmptyState(
            icon = Icons.Outlined.Insights,
            title = "No budgets this month",
            message = "Set a monthly limit from Budgets to track your spending here.",
        )
        is DashboardBudgetOverview.Progress -> DashboardBudgetProgress(budgetOverview.items)
    }
    Text(text = "Recent activity", style = MaterialTheme.typography.titleMedium)
    if (transactions.isEmpty()) {
        FinanceEmptyState(
            icon = Icons.Outlined.ReceiptLong,
            title = "No transactions this month",
            message = "New activity will appear here as you add it.",
        )
    } else {
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
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(totals: MonthTotals) {
    FinanceCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Total balance ${formatCents(totals.balanceCents)}" },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = "Total balance",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
        )
        Text(
            text = formatCents(totals.balanceCents),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = "Income minus expenses for this month",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    amountCents: Long,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    FinanceCard(
        modifier = modifier.semantics { contentDescription = "$label ${formatCents(amountCents)}" },
        contentPadding = 14.dp,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = accent.copy(alpha = 0.14f),
                contentColor = accent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(7.dp).size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatCents(amountCents),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DashboardBudgetProgress(progress: List<BudgetProgressItem>) {
    FinanceCard(
        contentPadding = 0.dp,
        verticalArrangement = Arrangement.Top,
    ) {
        progress.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            BudgetProgressRow(item)
        }
    }
}

@Composable
private fun BudgetProgressRow(item: BudgetProgressItem) {
    val status = item.statusPresentation()
    val categoryName = categoryPresentationName(item.categoryId)
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = categoryName, style = MaterialTheme.typography.titleMedium)
            FinanceStatusPill(text = status.label, color = status.color)
        }
        if (item.limitCents == null) {
            Text(
                text = "Spent ${formatCents(item.spentCents)} with no monthly limit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val percentage = ((item.spentCents.toDouble() / item.limitCents) * 100).toInt()
            FinanceProgressBar(
                fraction = item.spentCents.toFloat() / item.limitCents.toFloat(),
                color = status.color,
                description = "$categoryName budget progress: $percentage percent used",
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${formatCents(item.spentCents)} of ${formatCents(item.limitCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.remainingCents?.let(::budgetRemainingLabel).orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = status.color,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun BudgetProgressItem.statusPresentation(): BudgetStatus = when (state) {
    BudgetState.UNDER -> BudgetStatus("Under budget", FinanceIncome)
    BudgetState.AT_LIMIT -> BudgetStatus("At limit", FinanceWarning)
    BudgetState.OVER -> BudgetStatus("Over budget", FinanceExpense)
    BudgetState.NO_BUDGET -> BudgetStatus("No limit", FinanceTextMuted)
}

private fun budgetRemainingLabel(remainingCents: Long): String = when {
    remainingCents > 0 -> "${formatCents(remainingCents)} left"
    remainingCents == 0L -> "Limit reached"
    else -> "${formatCents(-remainingCents)} over"
}

private fun TransactionType.presentationName(): String = when (this) {
    TransactionType.INCOME -> "Income"
    TransactionType.EXPENSE -> "Expense"
}

private fun Transaction.presentationAmount(): String = when (type) {
    TransactionType.INCOME -> "+${formatCents(amountCents)}"
    TransactionType.EXPENSE -> "-${formatCents(amountCents)}"
}

private data class BudgetStatus(val label: String, val color: Color)
