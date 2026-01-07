package com.mengjizhang.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mengjizhang.app.data.local.AppDatabase
import com.mengjizhang.app.data.local.CategoryExpense
import com.mengjizhang.app.data.model.Record
import com.mengjizhang.app.data.model.expenseCategories
import com.mengjizhang.app.data.model.incomeCategories
import com.mengjizhang.app.data.repository.RecordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecordRepository(database.recordDao())
    }

    // 当前年月
    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // 最近记录
    val recentRecords: StateFlow<List<Record>> = repository.getRecentRecords(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当月记录
    val monthlyRecords: StateFlow<List<Record>> = combine(
        _currentYear,
        _currentMonth
    ) { year, month ->
        Pair(year, month)
    }.let { flow ->
        var currentFlow: StateFlow<List<Record>> = MutableStateFlow(emptyList())
        viewModelScope.launch {
            flow.collect { (year, month) ->
                repository.getRecordsByMonth(year, month).collect { records ->
                    (currentFlow as MutableStateFlow).value = records
                }
            }
        }
        currentFlow
    }

    // 当月支出
    private val _monthlyExpense = MutableStateFlow(0.0)
    val monthlyExpense: StateFlow<Double> = _monthlyExpense.asStateFlow()

    // 当月收入
    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    // 当月结余
    val monthlyBalance: StateFlow<Double> = combine(_monthlyIncome, _monthlyExpense) { income, expense ->
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // 分类统计
    private val _categoryStats = MutableStateFlow<List<CategoryExpense>>(emptyList())
    val categoryStats: StateFlow<List<CategoryExpense>> = _categoryStats.asStateFlow()

    init {
        loadMonthlyData()
    }

    /**
     * 加载当月数据
     */
    private fun loadMonthlyData() {
        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value

            // 加载支出
            repository.getMonthlyExpense(year, month).collect { expense ->
                _monthlyExpense.value = expense
            }
        }

        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value

            // 加载收入
            repository.getMonthlyIncome(year, month).collect { income ->
                _monthlyIncome.value = income
            }
        }

        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value

            // 加载分类统计
            repository.getExpenseByCategory(year, month).collect { stats ->
                _categoryStats.value = stats
            }
        }
    }

    /**
     * 切换月份
     */
    fun changeMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        loadMonthlyData()
    }

    /**
     * 上个月
     */
    fun previousMonth() {
        var year = _currentYear.value
        var month = _currentMonth.value - 1
        if (month < 1) {
            month = 12
            year -= 1
        }
        changeMonth(year, month)
    }

    /**
     * 下个月
     */
    fun nextMonth() {
        var year = _currentYear.value
        var month = _currentMonth.value + 1
        if (month > 12) {
            month = 1
            year += 1
        }
        changeMonth(year, month)
    }

    /**
     * 添加记录
     */
    fun addRecord(
        amount: Double,
        categoryId: Int,
        isExpense: Boolean,
        note: String = "",
        imagePath: String? = null,
        date: Long = System.currentTimeMillis(),
        inputMethod: String = "manual"
    ) {
        viewModelScope.launch {
            val categories = if (isExpense) expenseCategories else incomeCategories
            val category = categories.find { it.id == categoryId } ?: return@launch

            val record = Record(
                amount = amount,
                categoryId = categoryId,
                categoryName = category.name,
                categoryEmoji = category.emoji,
                isExpense = isExpense,
                note = note,
                imagePath = imagePath,
                date = date,
                inputMethod = inputMethod
            )

            repository.addRecord(record)
            loadMonthlyData()  // 刷新数据
        }
    }

    /**
     * 删除记录
     */
    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            loadMonthlyData()
        }
    }
}
