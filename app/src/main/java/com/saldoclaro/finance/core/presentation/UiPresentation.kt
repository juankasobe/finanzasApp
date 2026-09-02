package com.saldoclaro.finance.core.presentation

import androidx.annotation.StringRes
import com.saldoclaro.finance.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val SPANISH_LOCALE = Locale.forLanguageTag("es-ES")
private val USD = Currency.getInstance("USD")
private val SPANISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", SPANISH_LOCALE)

internal object UiPresentationResources {
    val categoryGroceries = R.string.category_groceries
    val categorySalary = R.string.category_salary
    val categoryUnknown = R.string.category_unknown
    val errorDataUnavailable = R.string.error_data_unavailable
    val errorOperationFailed = R.string.error_operation_failed
    val errorTargetUnavailable = R.string.error_target_unavailable
    val errorInvalidAmount = R.string.error_invalid_amount
}

enum class UiErrorKey(resourceId: Int) {
    DATA_UNAVAILABLE(UiPresentationResources.errorDataUnavailable),
    OPERATION_FAILED(UiPresentationResources.errorOperationFailed),
    TARGET_UNAVAILABLE(UiPresentationResources.errorTargetUnavailable),
    INVALID_AMOUNT(UiPresentationResources.errorInvalidAmount);

    @get:StringRes
    val resourceId: Int = resourceId
}

fun formatCents(cents: Long): String = NumberFormat.getCurrencyInstance(SPANISH_LOCALE).apply {
    currency = USD
}.format(BigDecimal.valueOf(cents, 2))

fun formatDate(date: LocalDate): String = date.format(SPANISH_DATE_FORMAT)

@StringRes
fun categoryResourceId(categoryId: String): Int? = when (categoryId.lowercase(Locale.ROOT)) {
    "builtin-groceries", "groceries" -> UiPresentationResources.categoryGroceries
    "builtin-salary", "salary" -> UiPresentationResources.categorySalary
    else -> null
}

fun categoryPresentationName(
    categoryId: String,
    persistedName: String? = null,
): String = categoryPresentationName(categoryId, persistedName) { resourceId ->
    when (resourceId) {
        UiPresentationResources.categoryGroceries -> "Supermercado"
        UiPresentationResources.categorySalary -> "Salario"
        else -> error("Unknown category resource: $resourceId")
    }
}

fun categoryPresentationName(
    categoryId: String,
    persistedName: String?,
    resolveResource: (Int) -> String,
): String {
    val resourceId = categoryResourceId(categoryId)
    if (resourceId != null) return resolveResource(resourceId)
    if (persistedName != null) return persistedName
    return "Categoría"
}
