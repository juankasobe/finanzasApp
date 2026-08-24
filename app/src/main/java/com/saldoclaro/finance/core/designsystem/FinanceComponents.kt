package com.saldoclaro.finance.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = verticalArrangement,
                content = content,
            )
        },
    )
}

@Composable
fun FinanceProgressBar(
    fraction: Float,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color),
        )
    }
}

@Composable
fun FinanceStatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = RoundedCornerShape(100.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun FinanceScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FinanceEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    FinanceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun FinanceTransactionRow(
    categoryKey: String,
    title: String,
    subtitle: String,
    amount: String,
    amountColor: Color,
    onDelete: (() -> Unit)? = null,
    deleteContentDescription: String = "Delete transaction",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryIconChip(categoryKey = categoryKey)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = amount,
            modifier = Modifier.widthIn(min = 64.dp),
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        onDelete?.let { delete ->
            IconButton(onClick = delete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = deleteContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CategoryIconChip(
    categoryKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val visual = categoryVisual(categoryKey)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(visual.color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = null,
            modifier = Modifier.size(size / 2),
            tint = visual.color,
        )
    }
}

fun formatCents(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.US)
    .format(BigDecimal.valueOf(cents, 2))

fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))

fun categoryPresentationName(categoryId: String): String {
    val cleaned = categoryId.removePrefix("builtin-").removePrefix("custom-")
    return cleaned
        .split('-', '_', ' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { word -> word.lowercase(Locale.US).replaceFirstChar { it.uppercase(Locale.US) } }
        .ifBlank { categoryId }
}

private data class CategoryVisual(val icon: ImageVector, val color: Color)

private fun categoryVisual(categoryKey: String): CategoryVisual {
    val key = categoryKey.lowercase(Locale.US)
    return when {
        key.contains("grocer") || key.contains("food") || key.contains("shop") ->
            CategoryVisual(Icons.Outlined.ShoppingBag, FinanceOrange)
        key.contains("salary") || key.contains("income") ->
            CategoryVisual(Icons.Outlined.AccountBalanceWallet, FinanceIncome)
        key.contains("transport") || key.contains("car") ->
            CategoryVisual(Icons.Outlined.DirectionsCar, FinanceSky)
        key.contains("home") || key.contains("housing") ->
            CategoryVisual(Icons.Outlined.Home, FinanceViolet)
        key.contains("movie") || key.contains("entertain") ->
            CategoryVisual(Icons.Outlined.Movie, FinancePink)
        key.contains("health") ->
            CategoryVisual(Icons.Outlined.FavoriteBorder, FinanceIncome)
        key.contains("subscription") ->
            CategoryVisual(Icons.Outlined.Replay, FinanceCyan)
        else -> CategoryVisual(Icons.Outlined.Category, FinanceMint)
    }
}
