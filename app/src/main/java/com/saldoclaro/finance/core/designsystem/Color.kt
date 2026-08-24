package com.saldoclaro.finance.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val FinanceBackground = Color(0xFF0E1116)
val FinanceSurface = Color(0xFF161B24)
val FinanceSurfaceRaised = Color(0xFF1B2130)
val FinanceOutline = Color(0xFF293241)
val FinanceText = Color(0xFFF1F4F9)
val FinanceTextSecondary = Color(0xFF98A2B3)
val FinanceTextMuted = Color(0xFF6F7A8D)

val FinanceMint = Color(0xFF2DD4A7)
val FinanceIncome = Color(0xFF34D399)
val FinanceExpense = Color(0xFFFB7185)
val FinanceWarning = Color(0xFFFBBF24)
val FinanceOrange = Color(0xFFFB923C)
val FinanceSky = Color(0xFF38BDF8)
val FinanceViolet = Color(0xFFA78BFA)
val FinancePink = Color(0xFFF472B6)
val FinanceCyan = Color(0xFF22D3EE)

val DarkColors = darkColorScheme(
    primary = FinanceMint,
    onPrimary = FinanceBackground,
    primaryContainer = Color(0xFF123F33),
    onPrimaryContainer = FinanceMint,
    secondary = Color(0xFFB4CCBD),
    onSecondary = FinanceBackground,
    secondaryContainer = Color(0xFF21372F),
    onSecondaryContainer = Color(0xFFD0E9D9),
    tertiary = FinanceWarning,
    onTertiary = FinanceBackground,
    background = FinanceBackground,
    onBackground = FinanceText,
    surface = FinanceSurface,
    onSurface = FinanceText,
    surfaceVariant = FinanceSurfaceRaised,
    onSurfaceVariant = FinanceTextSecondary,
    outline = FinanceOutline,
    error = FinanceExpense,
    onError = FinanceBackground,
    errorContainer = Color(0xFF48212B),
    onErrorContainer = Color(0xFFFFD9DF),
)
