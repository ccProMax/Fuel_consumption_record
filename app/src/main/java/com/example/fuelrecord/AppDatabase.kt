package com.example.fuelrecord

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import java.util.Date

/**
 * 日期类型转换器
 * 用于Room数据库存储Date类型
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * Room数据库抽象类
 * 定义数据库版本、实体和DAO
 */
@Database(
    entities = [FuelRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * 获取FuelRecordDao实例
     */
    abstract fun fuelRecordDao(): FuelRecordDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库单例实例
         * @param context 应用上下文
         * @return 数据库实例
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuel_record_database"
                )
                .fallbackToDestructiveMigration() // 数据库版本升级时，清空旧数据（仅用于开发）
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 获取内存数据库实例（用于测试）
         * @param context 应用上下文
         * @return 内存数据库实例
         */
        fun getInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).build()
        }
    }
}

/**
 * 数据库帮助类
 * 提供一些常用的数据库操作
 */
class DatabaseHelper(private val database: AppDatabase) {

    private val dao: FuelRecordDao by lazy {
        database.fuelRecordDao()
    }

    /**
     * 添加新的油耗记录
     * @param date 日期
     * @param mileage 里程
     * @param fuelAmount 加油量
     * @param pricePerLiter 单价
     * @param note 备注
     * @param fuelType 汽油标号
     * @param isFull 是否加满
     * @return 新记录的ID
     */
    suspend fun addFuelRecord(
        date: Date,
        mileage: Double,
        fuelAmount: Double,
        pricePerLiter: Double,
        note: String = "",
        fuelType: Int = 92,
        isFull: Boolean = true
    ): Long {
        // 获取所有记录（按里程升序排序）
        val allRecords = dao.getAllRecordsAsc().sortedBy { it.mileage }
        
        // 找到里程小于当前记录的最近一条记录
        var previousRecord: FuelRecord? = null
        for (rec in allRecords) {
            if (rec.mileage < mileage) {
                previousRecord = rec
            }
        }

        // 只有当前记录加满，且上一条记录也加满时，才计算油耗
        val consumption = if (isFull && previousRecord != null && previousRecord.isFull && mileage > previousRecord.mileage) {
            (fuelAmount / (mileage - previousRecord.mileage)) * 100.0
        } else {
            0.0
        }

        val record = FuelRecord(
            date = date,
            mileage = mileage,
            fuelAmount = fuelAmount,
            pricePerLiter = pricePerLiter,
            totalCost = fuelAmount * pricePerLiter,
            note = note,
            fuelConsumption = consumption,
            fuelType = fuelType,
            isFull = isFull
        )

        val recordId = dao.insert(record)
        
        // 重新计算该记录及后续记录的油耗
        recalculateConsumptionAfter(recordId)
        
        return recordId
    }

    /**
     * 更新油耗记录（需要重新计算后续记录的油耗）
     */
    suspend fun updateFuelRecord(record: FuelRecord): Boolean {
        dao.update(record)
        // 重新计算后续记录的油耗
        recalculateConsumptionAfter(record.id)
        return true
    }

    /**
     * 重新计算指定记录（包含）之后的所有记录的油耗
     */
    private suspend fun recalculateConsumptionAfter(fromId: Long) {
        // 按里程升序排序，确保能正确找到前驱记录
        val allRecords = dao.getAllRecordsAsc().sortedBy { it.mileage }
        var foundTarget = false
        var previousRecord: FuelRecord? = null

        for (rec in allRecords) {
            if (!foundTarget) {
                if (rec.id == fromId) {
                    foundTarget = true
                } else {
                    // 在找到目标记录之前，更新前驱记录
                    previousRecord = rec
                    continue
                }
            }

            // 重新计算油耗（包括目标记录本身及其后续记录）
            // 只有当前记录加满，且上一条记录也加满时，才计算油耗
            val newConsumption = if (rec.isFull && previousRecord != null && previousRecord.isFull && rec.mileage > previousRecord.mileage) {
                (rec.fuelAmount / (rec.mileage - previousRecord.mileage)) * 100.0
            } else {
                0.0
            }

            if (rec.fuelConsumption != newConsumption) {
                val updated = rec.copy(fuelConsumption = newConsumption)
                dao.update(updated)
            }

            // 更新前驱记录
            previousRecord = rec
        }
    }

    /**
     * 获取所有记录（按日期降序）
     */
    suspend fun getAllRecords(): List<FuelRecord> {
        return dao.getAllRecordsSync()
    }

    /**
     * 获取所有记录并附带行驶里程信息（按日期降序）
     */
    suspend fun getAllRecordsWithDistance(): List<FuelRecordWithDistance> {
        // DAO已经按 mileage ASC 排序，直接使用（用于计算行驶里程）
        val allRecordsAsc = dao.getAllRecordsAsc()
        val recordsMap = allRecordsAsc.associateBy { it.id }
        var previousRecord: FuelRecord? = null
        val result = mutableListOf<Pair<FuelRecord, FuelRecordWithDistance>>()

        for (rec in allRecordsAsc) {
            val distanceAdded = if (previousRecord != null && rec.mileage >= previousRecord.mileage) {
                rec.mileage - previousRecord.mileage
            } else {
                0.0
            }
            val withDist = FuelRecordWithDistance(
                record = rec,
                distanceAdded = distanceAdded,
                hasPreviousRecord = previousRecord != null
            )
            result.add(rec to withDist)
            previousRecord = rec
        }

        // 返回按里程降序（列表页从大到小）
        return result.sortedByDescending { it.first.mileage }.map { it.second }
    }

    /**
     * 删除记录（并重新计算油耗）
     */
    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
        // 删除后重新计算油耗
        recalculateAllConsumption()
    }

    /**
     * 更新记录（不重新计算油耗，由上层决定）
     */
    suspend fun updateRecord(record: FuelRecord) {
        dao.update(record)
    }

    /**
     * 获取统计信息
     */
    suspend fun getStatistics(): FuelStatistics {
        val totalRecords = dao.getRecordCount()
        val totalFuelAmount = dao.getTotalFuelAmount() ?: 0.0
        val totalCost = dao.getTotalCost() ?: 0.0
        val totalDistance = dao.getTotalDistance()
        val averageConsumption = dao.getAverageConsumption()
        val averagePrice = dao.getAveragePrice() ?: 0.0

        val allRecords = dao.getAllRecordsSync()
        val latestConsumption = if (allRecords.isNotEmpty()) {
            allRecords.first().fuelConsumption
        } else {
            0.0
        }

        val consumptionTrend = if (allRecords.size >= 2) {
            val previousConsumption = allRecords[1].fuelConsumption
            if (previousConsumption > 0) {
                ((latestConsumption - previousConsumption) / previousConsumption) * 100.0
            } else {
                0.0
            }
        } else {
            0.0
        }

        return FuelStatistics(
            totalRecords = totalRecords,
            totalFuelAmount = totalFuelAmount,
            totalCost = totalCost,
            totalDistance = totalDistance,
            averageConsumption = averageConsumption,
            averagePrice = averagePrice,
            latestConsumption = latestConsumption,
            consumptionTrend = consumptionTrend
        )
    }

    // ==================== 看板相关方法 ====================

    /**
     * 获取看板周期性统计数据（受时间周期影响）
     */
    suspend fun getDashboardPeriodStats(months: Int): DashboardPeriodStats {
        val startDate = getStartDateForMonths(months)

        val avgConsumption = dao.getAvgConsumptionInRange(startDate) ?: 0.0
        // 每百公里平均花费（元/100km）
        val avgCostPer100km = dao.getAvgCostPer100kmInRange(startDate) ?: 0.0
        // 转换为每公里花费（元/km）
        val avgPricePerKm = avgCostPer100km / 100.0

        val periodLabel = when {
            months == -1 -> "今年来"
            months == 1 -> "近1月"
            else -> "近${months}月"
        }

        return DashboardPeriodStats(
            periodLabel = periodLabel,
            avgConsumption = avgConsumption,
            avgPricePerKm = avgPricePerKm
        )
    }

    /**
     * 获取看板全局统计数据（不受时间周期影响）
     */
    suspend fun getDashboardGlobalStats(): DashboardGlobalStats {
        val totalDistance = dao.getTotalDistance()
        val totalFuel = dao.getTotalFuelAmount() ?: 0.0
        val totalCost = dao.getTotalCost() ?: 0.0
        val avgPrice = dao.getAveragePrice() ?: 0.0

        return DashboardGlobalStats(
            totalDistance = totalDistance,
            totalFuelAmount = totalFuel,
            totalCost = totalCost,
            avgPricePerLiter = avgPrice
        )
    }

    /**
     * 获取看板统计数据（已废弃，保留用于兼容）
     */
    @Deprecated("使用 getDashboardPeriodStats 和 getDashboardGlobalStats 替代")
    suspend fun getDashboardStats(months: Int): DashboardStats {
        val startDate = getStartDateForMonths(months)

        val avgConsumption = dao.getAvgConsumptionInRange(startDate) ?: 0.0
        // 每百公里平均花费（元/100km）
        val avgCostPer100km = dao.getAvgCostPer100kmInRange(startDate) ?: 0.0
        // 转换为每公里花费（元/km）
        val avgPricePerKm = avgCostPer100km / 100.0
        val totalDistance = dao.getTotalDistanceInRange(startDate)
        val totalFuel = dao.getTotalFuelAmountInRange(startDate)
        val totalCost = dao.getTotalCostInRange(startDate)
        val avgPrice = dao.getAvgPriceInRange(startDate) ?: 0.0

        val periodLabel = when {
            months == -1 -> "今年来"
            months == 1 -> "近1月"
            else -> "近${months}月"
        }

        return DashboardStats(
            periodLabel = periodLabel,
            avgConsumption = avgConsumption,
            avgPricePer100km = avgPricePerKm,  // 这里是每公里的价格（元/km）
            totalDistance = totalDistance,
            totalFuelAmount = totalFuel,
            totalCost = totalCost,
            avgPricePerLiter = avgPrice
        )
    }

    /**
     * 获取每百公里花费数据点（按每次加油记录）
     */
    suspend fun getCostPer100kmData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        val records = dao.getConsumptionRecords(startDate)
        val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        return records.map {
            val dateStr = sdf.format(java.util.Date(it.date))
            // 计算每百公里花费 = 油耗 * 单价
            val costPer100km = it.fuelConsumption * it.pricePerLiter
            ChartPoint(dateStr, costPer100km)
        }
    }

    /**
     * 获取油耗趋势数据点（按每次加油记录）
     */
    suspend fun getConsumptionData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        val records = dao.getConsumptionRecords(startDate)
        val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        return records.map {
            val dateStr = sdf.format(java.util.Date(it.date))
            ChartPoint(dateStr, it.fuelConsumption)
        }
    }

    /**
     * 获取油价趋势数据点（按每次加油记录）
     */
    suspend fun getFuelPriceData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        val records = dao.getPriceRecords(startDate)
        val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        return records.map {
            val dateStr = sdf.format(java.util.Date(it.date))
            ChartPoint(dateStr, it.pricePerLiter)
        }
    }

    /**
     * 获取每月加油量数据
     */
    suspend fun getMonthlyFuelData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        return dao.getMonthlyFuelAmount(startDate).map {
            ChartPoint(it.month, it.totalFuel)
        }
    }

    /**
     * 获取每月花费数据
     */
    suspend fun getMonthlyCostData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        return dao.getMonthlyCost(startDate).map {
            ChartPoint(it.month, it.totalCost)
        }
    }

    private fun getStartDateForMonths(months: Int): Long {
        val cal = java.util.Calendar.getInstance()
        if (months == -1) {
            // 今年来：设置为今年1月1日 00:00:00
            cal.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
        } else {
            cal.add(java.util.Calendar.MONTH, -months)
        }
        return cal.timeInMillis
    }

    /**
     * 清空所有记录
     */
    suspend fun clearAllRecords() {
        dao.deleteAll()
    }

    /**
     * 搜索记录
     */
    suspend fun searchRecords(keyword: String): List<FuelRecord> {
        return dao.searchRecords(keyword)
    }

    /**
     * 重新计算所有记录的油耗
     */
    private suspend fun recalculateAllConsumption() {
        // 按里程升序排序，确保能正确找到前驱记录
        val allRecords = dao.getAllRecordsAsc().sortedBy { it.mileage }
        var previousRecord: FuelRecord? = null

        for (rec in allRecords) {
            // 只有当前记录加满，且上一条记录也加满时，才计算油耗
            val newConsumption = if (rec.isFull && previousRecord != null && previousRecord.isFull && rec.mileage > previousRecord.mileage) {
                (rec.fuelAmount / (rec.mileage - previousRecord.mileage)) * 100.0
            } else {
                0.0
            }

            if (rec.fuelConsumption != newConsumption) {
                val updated = rec.copy(fuelConsumption = newConsumption)
                dao.update(updated)
            }

            // 更新前驱记录
            previousRecord = rec
        }
    }
}