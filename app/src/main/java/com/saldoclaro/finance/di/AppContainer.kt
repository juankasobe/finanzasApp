package com.saldoclaro.finance.di

import android.content.Context
import com.saldoclaro.finance.data.local.FinanceDatabase
import com.saldoclaro.finance.data.repository.RoomBudgetRepository
import com.saldoclaro.finance.data.repository.RoomFinanceRepositories
import com.saldoclaro.finance.feature.budgets.BudgetViewModel
import com.saldoclaro.finance.feature.categories.CategoryViewModel
import com.saldoclaro.finance.feature.dashboard.DashboardViewModel
import com.saldoclaro.finance.feature.transactions.TransactionViewModel
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

class AppContainer(context: Context) {
    private val zone = ZoneId.systemDefault()
    private val clock = Clock.system(zone)
    private val database = FinanceDatabase.open(context)
    private val transactions = RoomFinanceRepositories(database)
    private val budgets = RoomBudgetRepository(database)

    val dashboardViewModel by lazy { DashboardViewModel(transactions, budgets, clock, zone) }
    val transactionViewModel by lazy { TransactionViewModel(transactions, YearMonth.from(clock.instant().atZone(zone))) }
    val categoryViewModel by lazy { CategoryViewModel(transactions) }
    val budgetViewModel by lazy { BudgetViewModel(transactions, budgets, clock, zone) }
}
