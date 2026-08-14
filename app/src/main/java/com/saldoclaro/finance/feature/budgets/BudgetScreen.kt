package com.saldoclaro.finance.feature.budgets

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
import com.saldoclaro.finance.core.designsystem.RetryableErrorState
import com.saldoclaro.finance.data.local.CategoryEntity

@Composable
fun BudgetScreen(viewModel: BudgetViewModel, categories: List<CategoryEntity>) {
    val state by viewModel.state.collectAsState()
    var category by remember { mutableStateOf<CategoryEntity?>(null) }
    var limit by remember { mutableStateOf("") }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
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
        OutlinedTextField(limit, { limit = it }, label = { Text("Monthly limit") })
        Button(onClick = { category?.let { viewModel.saveLimit(it.id, limit) } }) { Text("Save limit") }
        when (val current = state) {
            is BudgetUiState.Content -> BudgetProgress(current.progress, categories)
            is BudgetUiState.Validation -> Text("Enter a positive amount with up to two decimal places")
            is BudgetUiState.Error -> RetryableErrorState(current.message, current.canRetry, viewModel::retry)
        }
    }
}

@Composable
private fun BudgetProgress(progress: List<BudgetProgressItem>, categories: List<CategoryEntity>) {
    val categoryNames = categories.associate { it.id to it.name }
    if (progress.isEmpty()) Text("No budget activity this month")
    progress.forEach { item ->
        ListItem(
            headlineContent = { Text("${categoryNames[item.categoryId] ?: item.categoryId}: ${item.state}") },
            supportingContent = { Text("Spent: ${item.spentCents} cents - Limit: ${item.limitCents ?: "Not set"} - Remaining: ${item.remainingCents ?: "Not available"}") },
        )
    }
}
