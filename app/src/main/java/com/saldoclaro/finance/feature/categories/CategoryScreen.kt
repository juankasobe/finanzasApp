package com.saldoclaro.finance.feature.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.saldoclaro.finance.R
import com.saldoclaro.finance.core.designsystem.CategoryIconChip
import com.saldoclaro.finance.core.designsystem.FinanceCard
import com.saldoclaro.finance.core.designsystem.FinanceEmptyState
import com.saldoclaro.finance.core.designsystem.FinanceScreenHeader
import com.saldoclaro.finance.core.designsystem.categoryPresentationName
import com.saldoclaro.finance.data.local.CategoryEntity

@Composable
fun CategoryScreen(viewModel: CategoryViewModel) {
    val state by viewModel.state.collectAsState()
    val mutation by viewModel.mutationState.collectAsState()
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
                CategoryCard(
                    category = category,
                    onEdit = viewModel::openEdit,
                    onDelete = viewModel::requestDelete,
                    onArchive = { viewModel.archive(it.id) },
                )
            }
        }
    }
    CategoryMutationHost(mutation, viewModel)
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onArchive: (CategoryEntity) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val displayName = categoryPresentationName(category.id, category.name)
    FinanceCard(
        modifier = Modifier.fillMaxWidth().height(152.dp).testTag("category-card-${category.id}"),
        containerColor = if (category.isArchived) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        contentPadding = 12.dp,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconChip(categoryKey = category.name)
            Spacer(Modifier.weight(1f))
            if (!category.isBuiltIn) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.category_manage, displayName),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = {
                        menuOpen = false; onEdit(category)
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete)) }, onClick = {
                        menuOpen = false; onDelete(category)
                    })
                    if (!category.isArchived) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_archive)) }, onClick = {
                            menuOpen = false; onArchive(category)
                        })
                    }
                }
            }
        }
        Text(
            text = displayName,
            modifier = Modifier.fillMaxWidth().testTag("category-title-${category.id}"),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (category.isBuiltIn) stringResource(R.string.category_builtin)
                else stringResource(R.string.category_custom),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (category.isArchived) Text(
                text = stringResource(R.string.category_archived),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryMutationHost(mutation: CategoryMutationState, viewModel: CategoryViewModel) {
    when (mutation) {
        CategoryMutationState.Idle -> Unit
        is CategoryMutationState.Editing -> RenameDialog(mutation.category, viewModel::submitRename, viewModel::dismissMutation)
        is CategoryMutationState.ConfirmDelete -> AlertDialog(
            onDismissRequest = viewModel::dismissMutation,
            title = { Text(stringResource(R.string.category_delete_title)) },
            text = { Text(stringResource(R.string.category_delete_message, mutation.category.name)) },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissMutation) { Text(stringResource(R.string.action_cancel)) } },
        )
        is CategoryMutationState.InUse -> AlertDialog(
            onDismissRequest = viewModel::dismissMutation,
            title = { Text(stringResource(R.string.category_in_use_title)) },
            text = { Text(stringResource(R.string.category_in_use_message)) },
            confirmButton = {
                TextButton(onClick = {
                    if (mutation.category.isArchived) viewModel.dismissMutation() else viewModel.archive(mutation.category.id)
                }) { Text(stringResource(if (mutation.category.isArchived) R.string.action_close else R.string.action_archive)) }
            },
            dismissButton = if (mutation.category.isArchived) null else {
                { TextButton(onClick = viewModel::dismissMutation) { Text(stringResource(R.string.action_cancel)) } }
            },
        )
        is CategoryMutationState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismissMutation,
            title = { Text(stringResource(R.string.category_error_title)) },
            text = { Text(stringResource(R.string.error_operation_failed)) },
            confirmButton = { TextButton(onClick = viewModel::dismissMutation) { Text(stringResource(R.string.action_close)) } },
        )
        is CategoryMutationState.Succeeded -> LaunchedEffect(mutation) { viewModel.dismissMutation() }
    }
}

@Composable
private fun RenameDialog(category: CategoryEntity, onSubmit: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(category.id, category.name) { mutableStateOf(category.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_edit_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().testTag("category-rename-input"),
                label = { Text(stringResource(R.string.category_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSubmit(name) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
