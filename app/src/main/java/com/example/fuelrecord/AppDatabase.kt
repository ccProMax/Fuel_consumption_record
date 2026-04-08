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
        
        // 找到里程小于当前记录的最近一条加满记录
        var previousFullRecord: FuelRecord? = null
        for (rec in allRecords) {
            if (rec.mileage < mileage && rec.isFull) {
                previousFullRecord = rec
            }
        }

        // 只有加满且有有效的前驱记录时才计算油耗
        val consumption = if (isFull && previousFullRecord != null && mileage > previousFullRecord.mileage) {
            (fuelAmount / (mileage - previousFullRecord.mileage)) * 100.0
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
        var previousFullRecord: FuelRecord? = null

        for (rec in allRecords) {
            if (!foundTarget) {
                // 在找到目标记录之前，更新previousFullRecord
                if (rec.id == fromId) {
                    foundTarget = true
                    // 继续处理当前记录，不要continue
                } else {
                    // 还没到目标记录，更新前驱记录（只要是加满的记录就可以作为前驱）
                    if (rec.isFull) {
                        previousFullRecord = rec
                    }
                    continue
                }
            }

            // 重新计算油耗（包括目标记录本身及其后续记录）
            val newConsumption = if (rec.isFull && previousFullRecord != null && rec.mileage > previousFullRecord.mileage) {
                (rec.fuelAmount / (rec.mileage - previousFullRecord.mileage)) * 100.0
            } else {
                0.0
            }

            if (rec.fuelConsumption != newConsumption) {
                val updated = rec.copy(fuelConsumption = newConsumption)
                dao.update(updated)
            }

            if (rec.isFull && newConsumption > 0) {
                previousFullRecord = rec.copy(fuelConsumption = newConsumption)
            }
        }
    }

    /**
     * 获取所有记录
     */
    suspend fun getAllRecords(): List<FuelRecord> {
        return dao.getAllRecordsSync()
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
     * 获取看板统计数据
     */
    suspend fun getDashboardStats(months: Int): DashboardStats {
        val startDate = getStartDateForMonths(months)

        val avgConsumption = dao.getAvgConsumptionInRange(startDate) ?: 0.0
        val avgCostPer100km = dao.getAvgCostPer100kmInRange(startDate) ?: 0.0
        val totalDistance = dao.getTotalDistanceInRange(startDate)
        val totalFuel = dao.getTotalFuelAmountInRange(startDate)
        val totalCost = dao.getTotalCostInRange(startDate)
        val avgPrice = dao.getAvgPriceInRange(startDate) ?: 0.0

        val periodLabel = if (months == 1) "近1月" else "近${months}月"

        return DashboardStats(
            periodLabel = periodLabel,
            avgConsumption = avgConsumption,
            avgPricePer100km = avgCostPer100km,
            totalDistance = totalDistance,
            totalFuelAmount = totalFuel,
            totalCost = totalCost,
            avgPricePerLiter = avgPrice
        )
    }

    /**
     * 获取每百公里花费数据点
     */
    suspend fun getCostPer100kmData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        return dao.getMonthlyCostPer100km(startDate).map {
            ChartPoint(it.month, it.avgCostPer100km)
        }
    }

    /**
     * 获取油耗趋势数据点
     */
    suspend fun getConsumptionData(months: Int): List<ChartPoint> {
        val startDate = getStartDateForMonths(months)
        return dao.getMonthlyAvgConsumption(startDate).map {
            ChartPoint(it.month, it.avgConsumption)
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
        cal.add(java.util.Calendar.MONTH, -months)
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
        var previousFullRecord: FuelRecord? = null

        for (rec in allRecords) {
            val newConsumption = if (rec.isFull && previousFullRecord != null && rec.mileage > previousFullRecord.mileage) {
                (rec.fuelAmount / (rec.mileage - previousFullRecord.mileage)) * 100.0
            } else {
                0.0
            }

            if (rec.fuelConsumption != newConsumption) {
                val updated = rec.copy(fuelConsumption = newConsumption)
                dao.update(updated)
            }

            if (rec.isFull && newConsumption > 0) {
                previousFullRecord = rec.copy(fuelConsumption = newConsumption)
            }
        }
    }
}