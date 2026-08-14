package com.saldoclaro.finance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.saldoclaro.finance.core.designsystem.SaldoClaroTheme
import com.saldoclaro.finance.navigation.SaldoClaroNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SaldoClaroTheme {
                Surface {
                    SaldoClaroNavHost((application as FinanceApplication).appContainer)
                }
            }
        }
    }
}
