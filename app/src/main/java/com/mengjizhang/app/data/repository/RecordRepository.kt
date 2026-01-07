package com.mengjizhang.app.data.repository

import com.mengjizhang.app.data.local.CategoryExpense
import com.mengjizhang.app.data.local.RecordDao
import com.mengjizhang.app.data.model.Record
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class RecordRepository(private val recordDao: RecordDao) {

    /**
     * 获取所有记录
     */
    fun getAllRecords(): Flow<List<Record>> = recordDao.getAllRecords()

    /**
     * 获取最近记录
     */
    fun getRecentRecords(limit: Int = 10): Flow<List<Record>> = recordDao.getRecentRecords(limit)

    /**
     * 获取指定月份的记录
     */
    fun getRecordsByMonth(year: Int, month: Int): Flow<List<Record>> {
        val (start, end) = getMonthRange(year, month)
        return recordDao.getRecordsByMonth(start, end)
    }

    /**
     * 获取指定月份的总支出
     */
    fun getMonthlyExpense(year: Int, month: Int): Flow<Double> {
        val (start, end) = getMonthRange(year, month)
        return recordDao.getMonthlyExpense(start, end)
    }

    /**
     * 获取指定月份的总收入
     */
    fun getMonthlyIncome(year: Int, month: Int): Flow<Double> {
        val (start, end) = getMonthRange(year, month)
        return recordDao.getMonthlyIncome(start, end)
    }

    /**
     * 获取分类支出统计
     */
    fun getExpenseByCategory(year: Int, month: Int): Flow<List<CategoryExpense>> {
        val (start, end) = getMonthRange(year, month)
        return recordDao.getExpenseByCategory(start, end)
    }

    /**
     * 添加记录
     */
    suspend fun addRecord(record: Record): Long = recordDao.insert(record)

    /**
     * 更新记录
     */
    suspend fun updateRecord(record: Record) = recordDao.update(record)

    /**
     * 删除记录
     */
    suspend fun deleteRecord(record: Record) = recordDao.delete(record)

    /**
     * 根据ID删除
     */
    suspend fun deleteRecordById(id: Long) = recordDao.deleteById(id)

    /**
     * 获取月份的起止时间戳
     */
    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // 月初
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // 下月初
        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return Pair(startOfMonth, endOfMonth)
    }

    /**
     * 获取今日的起止时间戳
     */
    fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // 今日开始
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        // 明日开始
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return Pair(startOfDay, endOfDay)
    }
}
