package com.saldoclaro.finance.feature.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun CategoryScreen(viewModel: CategoryViewModel) {
    val state by viewModel.state.collectAsState()
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category name") })
        Button(onClick = { viewModel.create(name) { name = "" } }) { Text("Add category") }
        state.categories.forEach { category ->
            ListItem(
                headlineContent = { Text(category.name) },
                supportingContent = { Text(if (category.isArchived) "Archived" else "Active") },
                trailingContent = if (category.isBuiltIn || category.isArchived) null else {
                    { Button(onClick = { viewModel.archive(category.id) }) { Text("Archive") } }
                },
            )
        }
        state.error?.let { Text(it) }
    }
}
