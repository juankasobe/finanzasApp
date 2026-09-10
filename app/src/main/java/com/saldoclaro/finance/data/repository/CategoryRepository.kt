package com.saldoclaro.finance.data.repository

import com.saldoclaro.finance.data.local.CategoryEntity
import kotlinx.coroutines.flow.Flow

enum class CategoryDeleteOutcome { Deleted, InUse }

interface CategoryRepository {
    fun observeCategories(): Flow<List<CategoryEntity>>
    suspend fun createCategory(name: String): Result<CategoryEntity>
    suspend fun renameCustomCategory(id: String, name: String): Result<Unit>
    suspend fun archiveCustomCategory(id: String): Result<Unit>
    suspend fun deleteCustomCategory(id: String): Result<CategoryDeleteOutcome>
}
