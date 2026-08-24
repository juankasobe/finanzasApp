package com.saldoclaro.finance.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun SaldoClaroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = SaldoClaroTypography,
        content = content,
    )
}
