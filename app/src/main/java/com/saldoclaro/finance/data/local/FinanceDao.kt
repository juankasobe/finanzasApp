package com.saldoclaro.finance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name") fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun find(id: String): CategoryEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(category: CategoryEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(categories: List<CategoryEntity>)
    @Query("UPDATE categories SET isArchived = 1 WHERE id = :id AND isBuiltIn = 0") suspend fun archiveCustom(id: String): Int
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(transaction: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE localDate BETWEEN :start AND :end ORDER BY localDate")
    fun observeMonth(start: LocalDate, end: LocalDate): Flow<List<TransactionEntity>>
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun delete(id: String): Int
    @Query("SELECT transactions.id, categories.name AS categoryName FROM transactions JOIN categories ON categoryId = categories.id ORDER BY localDate")
    fun observeAll(): Flow<List<TransactionWithCategory>>
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey ORDER BY categoryId")
    fun observeMonth(monthKey: String): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)
}
