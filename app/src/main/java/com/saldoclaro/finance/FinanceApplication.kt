package com.saldoclaro.finance

import android.app.Application
import com.saldoclaro.finance.di.AppContainer

class FinanceApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
