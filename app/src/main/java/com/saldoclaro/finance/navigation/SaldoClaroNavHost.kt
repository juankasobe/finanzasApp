package com.saldoclaro.finance.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saldoclaro.finance.R
import com.saldoclaro.finance.di.AppContainer
import com.saldoclaro.finance.feature.budgets.BudgetScreen
import com.saldoclaro.finance.feature.categories.CategoryScreen
import com.saldoclaro.finance.feature.dashboard.DashboardScreen
import com.saldoclaro.finance.feature.transactions.TransactionScreen

private data class Destination(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
)

private val destinations = listOf(
    Destination("dashboard", R.string.nav_dashboard, Icons.Outlined.Dashboard),
    Destination("transactions", R.string.nav_transactions, Icons.Outlined.ReceiptLong),
    Destination("categories", R.string.nav_categories, Icons.Outlined.Category),
    Destination("budgets", R.string.nav_budgets, Icons.Outlined.PieChart),
)

@Composable
fun SaldoClaroNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val categoryState by container.categoryViewModel.state.collectAsState()
    val allCategories = categoryState.categories
    val activeCategories = categoryState.categories.filterNot { it.isArchived }
    var transactionEntryRequest by remember { mutableStateOf(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentRoute?.destination?.route == "transactions") {
                        transactionEntryRequest += 1
                    } else {
                        navController.navigate("transactions") { launchSingleTop = true }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_add_transaction))
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            destinations.forEach { destination ->
                val label = stringResource(destination.label)
                val destinationDescription = stringResource(R.string.nav_navigate_to, label)
                NavigationBarItem(
                    modifier = Modifier.semantics {
                        contentDescription = destinationDescription
                    },
                    selected = currentRoute?.destination?.route == destination.route,
                    onClick = { navController.navigate(destination.route) { launchSingleTop = true } },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(label) },
                )
            }
            }
        },
    ) { padding ->
        NavHost(navController, "dashboard", Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(container.dashboardViewModel, allCategories) }
            composable("transactions") {
                TransactionScreen(
                    viewModel = container.transactionViewModel,
                    categories = activeCategories,
                    entryRequest = transactionEntryRequest,
                )
            }
            composable("categories") { CategoryScreen(container.categoryViewModel) }
            composable("budgets") { BudgetScreen(container.budgetViewModel, activeCategories) }
        }
    }
}
