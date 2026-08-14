package com.saldoclaro.finance.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.saldoclaro.finance.core.designsystem.RetryableErrorState
import com.saldoclaro.finance.domain.model.MonthTotals
import com.saldoclaro.finance.domain.model.Transaction

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        when (val current = state) {
            is DashboardUiState.Content -> DashboardContent(current.totals, current.recentActivity)
            is DashboardUiState.Empty -> {
                DashboardTotals(current.totals)
                Text("No transactions this month")
            }
            is DashboardUiState.Error -> RetryableErrorState(
                message = current.message,
                canRetry = current.canRetry,
                onRetry = viewModel::retry,
            )
        }
    }
}

@Composable
private fun DashboardContent(totals: MonthTotals, transactions: List<Transaction>) {
    DashboardTotals(totals)
    transactions.forEach { transaction ->
        ListItem(
            headlineContent = { Text("${transaction.type}: ${transaction.amountCents} cents") },
            supportingContent = { Text("${transaction.categoryId} · ${transaction.localDate}") },
        )
    }
}

@Composable
private fun DashboardTotals(totals: MonthTotals) {
    Text("Income: ${totals.incomeCents} cents")
    Text("Expenses: ${totals.expenseCents} cents")
    Text("Balance: ${totals.balanceCents} cents")
}
