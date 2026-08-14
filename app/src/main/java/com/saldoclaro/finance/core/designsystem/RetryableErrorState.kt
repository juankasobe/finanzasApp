package com.saldoclaro.finance.core.designsystem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RetryableErrorState(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    Text(message)
    if (canRetry) Button(onClick = onRetry) { Text("Retry") }
}
