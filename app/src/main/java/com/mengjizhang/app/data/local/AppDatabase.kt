package com.mengjizhang.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mengjizhang.app.data.model.Budget
import com.mengjizhang.app.data.model.Record

@Database(
    entities = [Record::class, Budget::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordDao(): RecordDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        const val DATABASE_NAME = "mengjizhang_database"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 关闭数据库连接（用于恢复备份前）
         */
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /**
         * 关闭数据库并执行 checkpoint，将 WAL 数据合并到主数据库文件
         * 用于备份前确保所有数据都写入主文件
         */
        fun closeDatabaseForBackup() {
            synchronized(this) {
                INSTANCE?.let { db ->
                    // 执行 checkpoint 将 WAL 数据写入主文件
                    try {
                        db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                    } catch (e: Exception) {
                        // 忽略错误，继续关闭
                    }
                    db.close()
                }
                INSTANCE = null
            }
        }
    }
}
