package com.saldoclaro.finance.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "categories", indices = [Index(value = ["normalizedName"], unique = true)])
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val isBuiltIn: Boolean,
    val isArchived: Boolean = false,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("categoryId")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amountCents: Long,
    val categoryId: String,
    val localDate: LocalDate,
)

@Entity(
    tableName = "budgets",
    primaryKeys = ["categoryId", "monthKey"],
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("categoryId")],
)
data class BudgetEntity(
    val categoryId: String,
    val monthKey: String,
    val limitCents: Long,
)

data class TransactionWithCategory(val id: String, val categoryName: String)
