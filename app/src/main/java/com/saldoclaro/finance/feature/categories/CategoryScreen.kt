package com.saldoclaro.finance.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.saldoclaro.finance.R
import com.saldoclaro.finance.core.designsystem.CategoryIconChip
import com.saldoclaro.finance.core.designsystem.FinanceCard
import com.saldoclaro.finance.core.designsystem.FinanceEmptyState
import com.saldoclaro.finance.core.designsystem.FinanceScreenHeader
import com.saldoclaro.finance.core.designsystem.FinanceStatusPill
import com.saldoclaro.finance.core.designsystem.FinanceTextMuted
import com.saldoclaro.finance.core.designsystem.categoryPresentationName
import com.saldoclaro.finance.data.local.CategoryEntity

@Composable
fun CategoryScreen(viewModel: CategoryViewModel) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 152.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            FinanceScreenHeader(
                title = stringResource(R.string.category_screen_title),
                subtitle = stringResource(R.string.category_screen_subtitle),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FinanceCard {
                Text(text = stringResource(R.string.category_create_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.create(name) { name = "" } },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text(text = stringResource(R.string.category_add), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        state.error?.let { reason ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                FinanceCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                    Text(
                        text = stringResource(reason.resourceId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        if (state.categories.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FinanceEmptyState(
                    icon = Icons.Outlined.FolderOff,
                    title = stringResource(R.string.category_empty_title),
                    message = stringResource(R.string.category_empty_message),
                )
            }
        } else {
            items(state.categories, key = { it.id }) { category ->
                CategoryCard(category = category, onArchive = viewModel::archive)
            }
        }
    }
}

@Composable
private fun CategoryCard(category: CategoryEntity, onArchive: (String) -> Unit) {
    FinanceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = if (category.isArchived) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconChip(categoryKey = category.name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = categoryPresentationName(category.id, category.name), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (category.isBuiltIn) stringResource(R.string.category_builtin)
                    else stringResource(R.string.category_custom),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                category.isArchived -> FinanceStatusPill(
                    text = stringResource(R.string.category_archived),
                    color = FinanceTextMuted,
                )
                !category.isBuiltIn -> IconButton(onClick = { onArchive(category.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Archive,
                        contentDescription = stringResource(
                            R.string.category_archive,
                            categoryPresentationName(category.id, category.name),
                        ),
                    )
                }
            }
        }
    }
}
