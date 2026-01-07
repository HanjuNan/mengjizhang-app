package com.mengjizhang.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mengjizhang.app.data.local.AppDatabase
import com.mengjizhang.app.data.local.CategoryExpense
import com.mengjizhang.app.data.model.BudgetStatus
import com.mengjizhang.app.data.model.Record
import com.mengjizhang.app.data.model.expenseCategories
import com.mengjizhang.app.data.model.incomeCategories
import com.mengjizhang.app.data.repository.BudgetRepository
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
    private val budgetRepository: BudgetRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RecordRepository(database.recordDao())
        budgetRepository = BudgetRepository(database.budgetDao())
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

    // 当月预算状态
    private val _budgetStatus = MutableStateFlow(BudgetStatus.Empty)
    val budgetStatus: StateFlow<BudgetStatus> = _budgetStatus.asStateFlow()

    // 当月预算金额
    private val _monthlyBudget = MutableStateFlow(0.0)
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    init {
        loadMonthlyData()
        loadBudgetData()
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
     * 加载当月预算数据
     */
    private fun loadBudgetData() {
        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value

            budgetRepository.getBudgetByMonth(year, month).collect { budget ->
                _monthlyBudget.value = budget?.monthlyBudget ?: 0.0
                updateBudgetStatus()
            }
        }
    }

    /**
     * 更新预算状态
     */
    private fun updateBudgetStatus() {
        val budget = _monthlyBudget.value
        val spent = _monthlyExpense.value

        if (budget <= 0) {
            _budgetStatus.value = BudgetStatus.Empty
            return
        }

        val remaining = budget - spent
        val percentage = (spent / budget).toFloat().coerceIn(0f, 1.5f)
        val isOverBudget = spent > budget

        _budgetStatus.value = BudgetStatus(
            budget = budget,
            spent = spent,
            remaining = remaining,
            percentage = percentage,
            isOverBudget = isOverBudget
        )
    }

    /**
     * 设置当月预算
     */
    fun setBudget(amount: Double) {
        viewModelScope.launch {
            budgetRepository.setBudget(_currentYear.value, _currentMonth.value, amount)
            loadBudgetData()
        }
    }

    /**
     * 获取推荐预算（上月预算）
     */
    suspend fun getRecommendedBudget(): Double {
        return budgetRepository.getLatestBudget()?.monthlyBudget ?: 0.0
    }

    /**
     * 切换月份
     */
    fun changeMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        loadMonthlyData()
        loadBudgetData()
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

    /**
     * 更新记录
     */
    fun updateRecord(
        recordId: Long,
        amount: Double,
        categoryId: Int,
        isExpense: Boolean,
        note: String = "",
        imagePath: String? = null,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val categories = if (isExpense) expenseCategories else incomeCategories
            val category = categories.find { it.id == categoryId } ?: return@launch

            val record = Record(
                id = recordId,
                amount = amount,
                categoryId = categoryId,
                categoryName = category.name,
                categoryEmoji = category.emoji,
                isExpense = isExpense,
                note = note,
                imagePath = imagePath,
                date = date,
                inputMethod = "manual"  // 编辑后统一为手动
            )

            repository.updateRecord(record)
            loadMonthlyData()
        }
    }

    /**
     * 根据ID获取记录
     */
    fun getRecordById(id: Long): Record? {
        return recentRecords.value.find { it.id == id }
            ?: monthlyRecords.value.find { it.id == id }
    }

    // ========== 搜索功能 ==========

    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    // 搜索结果
    private val _searchResults = MutableStateFlow<List<Record>>(emptyList())
    val searchResults: StateFlow<List<Record>> = _searchResults.asStateFlow()

    // 搜索状态
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // 筛选条件
    private val _searchStartDate = MutableStateFlow<Long?>(null)
    val searchStartDate: StateFlow<Long?> = _searchStartDate.asStateFlow()

    private val _searchEndDate = MutableStateFlow<Long?>(null)
    val searchEndDate: StateFlow<Long?> = _searchEndDate.asStateFlow()

    private val _searchMinAmount = MutableStateFlow<Double?>(null)
    val searchMinAmount: StateFlow<Double?> = _searchMinAmount.asStateFlow()

    private val _searchMaxAmount = MutableStateFlow<Double?>(null)
    val searchMaxAmount: StateFlow<Double?> = _searchMaxAmount.asStateFlow()

    private val _searchIsExpense = MutableStateFlow<Boolean?>(null)
    val searchIsExpense: StateFlow<Boolean?> = _searchIsExpense.asStateFlow()

    /**
     * 更新搜索关键词
     */
    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    /**
     * 设置日期筛选
     */
    fun setSearchDateRange(startDate: Long?, endDate: Long?) {
        _searchStartDate.value = startDate
        _searchEndDate.value = endDate
    }

    /**
     * 设置金额筛选
     */
    fun setSearchAmountRange(minAmount: Double?, maxAmount: Double?) {
        _searchMinAmount.value = minAmount
        _searchMaxAmount.value = maxAmount
    }

    /**
     * 设置类型筛选
     */
    fun setSearchType(isExpense: Boolean?) {
        _searchIsExpense.value = isExpense
    }

    /**
     * 执行搜索
     */
    fun performSearch() {
        viewModelScope.launch {
            _isSearching.value = true
            repository.advancedSearch(
                keyword = _searchKeyword.value,
                startDate = _searchStartDate.value,
                endDate = _searchEndDate.value,
                minAmount = _searchMinAmount.value,
                maxAmount = _searchMaxAmount.value,
                isExpense = _searchIsExpense.value
            ).collect { results ->
                _searchResults.value = results
                _isSearching.value = false
            }
        }
    }

    /**
     * 清空搜索条件
     */
    fun clearSearch() {
        _searchKeyword.value = ""
        _searchStartDate.value = null
        _searchEndDate.value = null
        _searchMinAmount.value = null
        _searchMaxAmount.value = null
        _searchIsExpense.value = null
        _searchResults.value = emptyList()
    }

    // ========== 图表统计功能 ==========

    /**
     * 获取过去7天的每日数据
     */
    suspend fun getWeeklyData(): List<DailyStats> {
        val result = mutableListOf<DailyStats>()
        val calendar = Calendar.getInstance()

        // 从6天前开始到今天
        for (i in 6 downTo 0) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.add(Calendar.DAY_OF_MONTH, -i)
            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            dayCalendar.set(Calendar.SECOND, 0)
            dayCalendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = dayCalendar.timeInMillis

            dayCalendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = dayCalendar.timeInMillis

            val expense = repository.getDailyExpense(startOfDay, endOfDay)
            val income = repository.getDailyIncome(startOfDay, endOfDay)

            val dayOfWeek = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
            dayCalendar.add(Calendar.DAY_OF_MONTH, -1)
            val label = dayOfWeek[dayCalendar.get(Calendar.DAY_OF_WEEK) - 1]

            result.add(DailyStats(label, expense, income))
        }

        return result
    }

    /**
     * 获取过去6个月的月度数据
     */
    suspend fun getMonthlyTrendData(): List<MonthlyStats> {
        val result = mutableListOf<MonthlyStats>()
        val calendar = Calendar.getInstance()

        for (i in 5 downTo 0) {
            val monthCalendar = Calendar.getInstance()
            monthCalendar.add(Calendar.MONTH, -i)

            val year = monthCalendar.get(Calendar.YEAR)
            val month = monthCalendar.get(Calendar.MONTH) + 1

            monthCalendar.set(Calendar.DAY_OF_MONTH, 1)
            monthCalendar.set(Calendar.HOUR_OF_DAY, 0)
            monthCalendar.set(Calendar.MINUTE, 0)
            monthCalendar.set(Calendar.SECOND, 0)
            monthCalendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = monthCalendar.timeInMillis

            monthCalendar.add(Calendar.MONTH, 1)
            val endOfMonth = monthCalendar.timeInMillis

            val expense = repository.getDailyExpense(startOfMonth, endOfMonth)
            val income = repository.getDailyIncome(startOfMonth, endOfMonth)

            result.add(MonthlyStats("${month}月", expense, income))
        }

        return result
    }
}

/**
 * 每日统计数据
 */
data class DailyStats(
    val label: String,
    val expense: Double,
    val income: Double
)

/**
 * 月度统计数据
 */
data class MonthlyStats(
    val label: String,
    val expense: Double,
    val income: Double
)
