package com.mengjizhang.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mengjizhang.app.data.model.Record
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    /**
     * 插入记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: Record): Long

    /**
     * 更新记录
     */
    @Update
    suspend fun update(record: Record)

    /**
     * 删除记录
     */
    @Delete
    suspend fun delete(record: Record)

    /**
     * 根据ID删除
     */
    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有记录（按日期降序）
     */
    @Query("SELECT * FROM records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<Record>>

    /**
     * 根据ID获取记录
     */
    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getRecordById(id: Long): Record?

    /**
     * 获取指定日期范围的记录
     */
    @Query("SELECT * FROM records WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getRecordsByDateRange(startDate: Long, endDate: Long): Flow<List<Record>>

    /**
     * 获取指定月份的记录
     */
    @Query("SELECT * FROM records WHERE date >= :startOfMonth AND date < :endOfMonth ORDER BY date DESC")
    fun getRecordsByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Record>>

    /**
     * 获取最近N条记录
     */
    @Query("SELECT * FROM records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<Record>>

    /**
     * 获取指定月份的总支出
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM records WHERE isExpense = 1 AND date >= :startOfMonth AND date < :endOfMonth")
    fun getMonthlyExpense(startOfMonth: Long, endOfMonth: Long): Flow<Double>

    /**
     * 获取指定月份的总收入
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM records WHERE isExpense = 0 AND date >= :startOfMonth AND date < :endOfMonth")
    fun getMonthlyIncome(startOfMonth: Long, endOfMonth: Long): Flow<Double>

    /**
     * 按分类统计支出
     */
    @Query("""
        SELECT categoryId, categoryName, categoryEmoji,
               SUM(amount) as totalAmount, COUNT(*) as count
        FROM records
        WHERE isExpense = 1 AND date >= :startOfMonth AND date < :endOfMonth
        GROUP BY categoryId
        ORDER BY totalAmount DESC
    """)
    fun getExpenseByCategory(startOfMonth: Long, endOfMonth: Long): Flow<List<CategoryExpense>>

    /**
     * 删除所有记录
     */
    @Query("DELETE FROM records")
    suspend fun deleteAll()

    /**
     * 搜索记录（按关键词匹配备注或分类名称）
     */
    @Query("""
        SELECT * FROM records
        WHERE note LIKE '%' || :keyword || '%'
           OR categoryName LIKE '%' || :keyword || '%'
        ORDER BY date DESC
    """)
    fun searchRecords(keyword: String): Flow<List<Record>>

    /**
     * 高级搜索（支持日期范围、金额范围、类型筛选）
     */
    @Query("""
        SELECT * FROM records
        WHERE (:keyword = '' OR note LIKE '%' || :keyword || '%' OR categoryName LIKE '%' || :keyword || '%')
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date < :endDate)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:isExpense IS NULL OR isExpense = :isExpense)
        ORDER BY date DESC
    """)
    fun advancedSearch(
        keyword: String = "",
        startDate: Long? = null,
        endDate: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        isExpense: Boolean? = null
    ): Flow<List<Record>>

    /**
     * 获取日期范围内的每日支出汇总
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM records
        WHERE isExpense = 1 AND date >= :startDate AND date < :endDate
    """)
    suspend fun getDailyExpense(startDate: Long, endDate: Long): Double

    /**
     * 获取日期范围内的每日收入汇总
     */
    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM records
        WHERE isExpense = 0 AND date >= :startDate AND date < :endDate
    """)
    suspend fun getDailyIncome(startDate: Long, endDate: Long): Double

    /**
     * 同步获取指定日期范围的记录（供小组件使用）
     */
    @Query("SELECT * FROM records WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getRecordsBetweenDatesSync(startDate: Long, endDate: Long): List<Record>

    /**
     * 获取总记录数
     */
    @Query("SELECT COUNT(*) FROM records")
    fun getTotalRecordCount(): Flow<Int>

    /**
     * 获取有记录的天数（不重复的日期数量）
     */
    @Query("SELECT COUNT(DISTINCT date(date/1000, 'unixepoch', 'localtime')) FROM records")
    fun getRecordingDaysCount(): Flow<Int>

    /**
     * 获取所有记录日期（用于计算连续打卡）
     */
    @Query("SELECT DISTINCT date(date/1000, 'unixepoch', 'localtime') as recordDate FROM records ORDER BY recordDate DESC")
    suspend fun getAllRecordDates(): List<String>
}

/**
 * 分类支出统计结果
 */
data class CategoryExpense(
    val categoryId: Int,
    val categoryName: String,
    val categoryEmoji: String,
    val totalAmount: Double,
    val count: Int
)
