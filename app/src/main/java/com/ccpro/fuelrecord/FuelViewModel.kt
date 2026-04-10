package com.ccpro.fuelrecord

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * 油耗记录视图模型
 */
class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseHelper: DatabaseHelper
    private val _records = MutableLiveData<List<FuelRecordWithDistance>>()
    private val _statistics = MutableLiveData<FuelStatistics>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _errorMessage = MutableLiveData<String?>()

    // 看板数据
    private val _dashboardStats = MutableLiveData<DashboardStats>()
    private val _dashboardPeriodStats = MutableLiveData<DashboardPeriodStats>()
    private val _dashboardGlobalStats = MutableLiveData<DashboardGlobalStats>()
    private val _consumptionChartData = MutableLiveData<List<ChartPoint>>()
    private val _fuelPriceChartData = MutableLiveData<List<ChartPoint>>()
    private val _costPer100kmChartData = MutableLiveData<List<ChartPoint>>()
    private val _monthlyFuelChartData = MutableLiveData<List<ChartPoint>>()
    private val _monthlyCostChartData = MutableLiveData<List<ChartPoint>>()

    val records: LiveData<List<FuelRecordWithDistance>> get() = _records
    val statistics: LiveData<FuelStatistics> get() = _statistics
    val isLoading: LiveData<Boolean> get() = _isLoading
    val errorMessage: LiveData<String?> get() = _errorMessage
    val dashboardStats: LiveData<DashboardStats> get() = _dashboardStats
    val dashboardPeriodStats: LiveData<DashboardPeriodStats> get() = _dashboardPeriodStats
    val dashboardGlobalStats: LiveData<DashboardGlobalStats> get() = _dashboardGlobalStats
    val consumptionChartData: LiveData<List<ChartPoint>> get() = _consumptionChartData
    val fuelPriceChartData: LiveData<List<ChartPoint>> get() = _fuelPriceChartData
    val costPer100kmChartData: LiveData<List<ChartPoint>> get() = _costPer100kmChartData
    val monthlyFuelChartData: LiveData<List<ChartPoint>> get() = _monthlyFuelChartData
    val monthlyCostChartData: LiveData<List<ChartPoint>> get() = _monthlyCostChartData

    private val prefs: SharedPreferences by lazy {
        application.getSharedPreferences("fuel_record_prefs", Context.MODE_PRIVATE)
    }

    // 用户设置的看板统计月数
    var dashboardMonths: Int
        get() = prefs.getInt("dashboard_months", 6)
        set(value) {
            prefs.edit().putInt("dashboard_months", value).apply()
            // 直接传递新值，避免异步读取时可能还未写入的问题
            loadDashboardData(value)
        }

    init {
        val database = AppDatabase.getDatabase(application)
        databaseHelper = DatabaseHelper(database)
        loadAllRecords()
        loadStatistics()
        loadDashboardData()
    }

    fun loadAllRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _records.value = databaseHelper.getAllRecordsWithDistance()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "加载记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                _statistics.value = databaseHelper.getStatistics()
            } catch (e: Exception) {
                _errorMessage.value = "加载统计信息失败: ${e.message}"
            }
        }
    }

    /**
     * 添加记录
     */
    fun addRecord(
        date: Date,
        mileage: Double,
        fuelAmount: Double,
        pricePerLiter: Double,
        note: String = "",
        fuelType: Int = 92,
        isFull: Boolean = true
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (mileage <= 0) {
                    _errorMessage.value = "里程必须大于0"
                    return@launch
                }
                if (fuelAmount <= 0) {
                    _errorMessage.value = "加油量必须大于0"
                    return@launch
                }
                if (pricePerLiter <= 0) {
                    _errorMessage.value = "单价必须大于0"
                    return@launch
                }

                val recordId = databaseHelper.addFuelRecord(
                    date = date,
                    mileage = mileage,
                    fuelAmount = fuelAmount,
                    pricePerLiter = pricePerLiter,
                    note = note,
                    fuelType = fuelType,
                    isFull = isFull
                )
                if (recordId > 0) {
                    loadAllRecords()
                    loadStatistics()
                    loadDashboardData()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "添加记录失败"
                }
            } catch (e: Exception) {
                _errorMessage.value = "添加记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新记录
     */
    fun updateRecord(record: FuelRecord) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                databaseHelper.updateFuelRecord(record)
                loadAllRecords()
                loadStatistics()
                loadDashboardData()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "更新记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                databaseHelper.deleteRecord(recordId)
                loadAllRecords()
                loadStatistics()
                loadDashboardData()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "删除记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 加载看板数据
     */
    fun loadDashboardData(months: Int = dashboardMonths) {
        viewModelScope.launch {
            try {
                // 加载周期性数据（受时间周期影响）
                _dashboardPeriodStats.value = databaseHelper.getDashboardPeriodStats(months)
                _consumptionChartData.value = databaseHelper.getConsumptionData(months)
                _fuelPriceChartData.value = databaseHelper.getFuelPriceData(months)
                _costPer100kmChartData.value = databaseHelper.getCostPer100kmData(months)
                _monthlyFuelChartData.value = databaseHelper.getMonthlyFuelData(months)
                _monthlyCostChartData.value = databaseHelper.getMonthlyCostData(months)

                // 加载全局数据（不受时间周期影响）
                _dashboardGlobalStats.value = databaseHelper.getDashboardGlobalStats()

                // 保留旧接口兼容
                _dashboardStats.value = databaseHelper.getDashboardStats(months)
            } catch (e: Exception) {
                _errorMessage.value = "加载看板数据失败: ${e.message}"
            }
        }
    }

    fun searchRecords(keyword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val found = databaseHelper.searchRecords(keyword)
                _records.value = found.map { FuelRecordWithDistance(it) }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "搜索记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAllRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                databaseHelper.clearAllRecords()
                loadAllRecords()
                loadStatistics()
                loadDashboardData()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "清空记录失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * 获取所有记录（用于同步）
     */
    suspend fun getAllRecords(): List<FuelRecord> {
        return databaseHelper.getAllRecords()
    }

    /**
     * 同步获取所有记录（按里程降序）
     */
    fun getAllRecordsSync(): List<FuelRecord> {
        return runBlocking {
            databaseHelper.getAllRecords()
        }
    }

    /**
     * 插入记录（用于同步）
     */
    suspend fun insertRecord(record: FuelRecord) {
        databaseHelper.insertRecord(record)
    }
}
