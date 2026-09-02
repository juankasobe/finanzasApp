package com.saldoclaro.finance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CategoryEntity::class, TransactionEntity::class, BudgetEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        fun open(context: Context): FinanceDatabase = Room.databaseBuilder(
            context.applicationContext,
            FinanceDatabase::class.java,
            "saldo-claro.db",
        ).addCallback(seedCallback).build()

        fun inMemory(context: Context): FinanceDatabase =
            Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).allowMainThreadQueries().build().also(::seedBuiltIns)

        private val seedCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO categories VALUES ('builtin-groceries', 'Supermercado', 'groceries', 1, 0)")
                db.execSQL("INSERT INTO categories VALUES ('builtin-salary', 'Salario', 'salary', 1, 0)")
            }
        }

        private fun seedBuiltIns(database: FinanceDatabase) {
            database.openHelper.writableDatabase.apply {
                execSQL("INSERT INTO categories VALUES ('builtin-groceries', 'Supermercado', 'groceries', 1, 0)")
                execSQL("INSERT INTO categories VALUES ('builtin-salary', 'Salario', 'salary', 1, 0)")
            }
        }
    }
}
