package com.saldoclaro.finance.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saldoclaro.finance.di.AppContainer
import com.saldoclaro.finance.feature.budgets.BudgetScreen
import com.saldoclaro.finance.feature.categories.CategoryScreen
import com.saldoclaro.finance.feature.dashboard.DashboardScreen
import com.saldoclaro.finance.feature.transactions.TransactionScreen

private data class Destination(val route: String, val label: String)

private val destinations = listOf(
    Destination("dashboard", "Dashboard"),
    Destination("transactions", "Transactions"),
    Destination("categories", "Categories"),
    Destination("budgets", "Budgets"),
)

@Composable
fun SaldoClaroNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val categoryState by container.categoryViewModel.state.collectAsState()
    val activeCategories = categoryState.categories.filterNot { it.isArchived }
    Scaffold(bottomBar = {
        NavigationBar {
            destinations.forEach { destination ->
                NavigationBarItem(
                    modifier = Modifier.semantics {
                        contentDescription = "Navigate to ${destination.label}"
                    },
                    selected = currentRoute?.destination?.route == destination.route,
                    onClick = { navController.navigate(destination.route) { launchSingleTop = true } },
                    icon = { Text(destination.label.first().toString()) },
                    label = { Text(destination.label) },
                )
            }
        }
    }) { padding ->
        NavHost(navController, "dashboard", Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(container.dashboardViewModel) }
            composable("transactions") { TransactionScreen(container.transactionViewModel, activeCategories) }
            composable("categories") { CategoryScreen(container.categoryViewModel) }
            composable("budgets") { BudgetScreen(container.budgetViewModel, activeCategories) }
        }
    }
}
