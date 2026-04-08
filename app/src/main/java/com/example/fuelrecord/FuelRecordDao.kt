package com.example.fuelrecord

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * 油耗记录数据访问对象
 */
@Dao
interface FuelRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FuelRecord): Long

    @Update
    suspend fun update(record: FuelRecord)

    @Delete
    suspend fun delete(record: FuelRecord)

    @Query("DELETE FROM fuel_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM fuel_records ORDER BY mileage DESC")
    fun getAllRecords(): LiveData<List<FuelRecord>>

    @Query("SELECT * FROM fuel_records ORDER BY mileage DESC")
    suspend fun getAllRecordsSync(): List<FuelRecord>

    @Query("SELECT * FROM fuel_records ORDER BY mileage ASC")
    suspend fun getAllRecordsAsc(): List<FuelRecord>

    @Query("SELECT * FROM fuel_records WHERE id = :id")
    suspend fun getRecordById(id: Long): FuelRecord?

    @Query("SELECT * FROM fuel_records ORDER BY date DESC LIMIT 1")
    suspend fun getLastRecord(): FuelRecord?

    @Query("SELECT * FROM fuel_records ORDER BY date ASC LIMIT 1")
    suspend fun getFirstRecord(): FuelRecord?

    @Query("SELECT COUNT(*) FROM fuel_records")
    suspend fun getRecordCount(): Int

    @Query("SELECT SUM(fuelAmount) FROM fuel_records")
    suspend fun getTotalFuelAmount(): Double?

    @Query("SELECT SUM(totalCost) FROM fuel_records")
    suspend fun getTotalCost(): Double?

    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(*) >= 2 THEN 
                    (SELECT mileage FROM fuel_records ORDER BY date DESC LIMIT 1) - 
                    (SELECT mileage FROM fuel_records ORDER BY date ASC LIMIT 1)
                ELSE 0.0 
            END 
        FROM fuel_records
    """)
    suspend fun getTotalDistance(): Double

    @Query("""
        SELECT 
            CASE 
                WHEN (SELECT mileage FROM fuel_records ORDER BY date DESC LIMIT 1) - 
                     (SELECT mileage FROM fuel_records ORDER BY date ASC LIMIT 1) > 0 
                THEN 
                    (SELECT SUM(fuelAmount) FROM fuel_records) / 
                    ((SELECT mileage FROM fuel_records ORDER BY date DESC LIMIT 1) - 
                     (SELECT mileage FROM fuel_records ORDER BY date ASC LIMIT 1)) * 100
                ELSE 0.0 
            END 
        FROM fuel_records
        WHERE (SELECT COUNT(*) FROM fuel_records) >= 2
    """)
    suspend fun getAverageConsumption(): Double

    @Query("SELECT AVG(pricePerLiter) FROM fuel_records")
    suspend fun getAveragePrice(): Double?

    @Query("DELETE FROM fuel_records")
    suspend fun deleteAll()

    @Query("SELECT * FROM fuel_records WHERE note LIKE '%' || :keyword || '%' ORDER BY date DESC")
    suspend fun searchRecords(keyword: String): List<FuelRecord>

    @Query("SELECT * FROM fuel_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getRecordsByDateRange(startDate: Long, endDate: Long): List<FuelRecord>

    // ==================== 看板相关查询 ====================

    @Query("""
        SELECT * FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1
        ORDER BY date ASC
    """)
    suspend fun getFullFillRecordsInRange(startDate: Long): List<FuelRecord>

    @Query("""
        SELECT * FROM fuel_records 
        WHERE date >= :startDate 
        ORDER BY date ASC
    """)
    suspend fun getRecordsInRangeAsc(startDate: Long): List<FuelRecord>

    /**
     * 按月分组统计：每月的总加油量
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', date/1000, 'unixepoch') AS month,
            SUM(fuelAmount) AS totalFuel
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1
        GROUP BY month 
        ORDER BY month ASC
    """)
    suspend fun getMonthlyFuelAmount(startDate: Long): List<MonthlyFuelData>

    /**
     * 按月分组统计：每月的总金额
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', date/1000, 'unixepoch') AS month,
            SUM(totalCost) AS totalCost
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1
        GROUP BY month 
        ORDER BY month ASC
    """)
    suspend fun getMonthlyCost(startDate: Long): List<MonthlyCostData>

    /**
     * 按月分组统计：每月的平均油耗（基于加满记录）
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', date/1000, 'unixepoch') AS month,
            AVG(fuelConsumption) AS avgConsumption
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1 AND fuelConsumption > 0
        GROUP BY month 
        ORDER BY month ASC
    """)
    suspend fun getMonthlyAvgConsumption(startDate: Long): List<MonthlyConsumptionData>

    /**
     * 按月分组统计：每月的每百公里花费
     */
    @Query("""
        SELECT 
            strftime('%Y-%m', date/1000, 'unixepoch') AS month,
            AVG(fuelConsumption * pricePerLiter) AS avgCostPer100km
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1 AND fuelConsumption > 0
        GROUP BY month 
        ORDER BY month ASC
    """)
    suspend fun getMonthlyCostPer100km(startDate: Long): List<MonthlyCostPer100kmData>

    /**
     * 获取近N个月的平均油耗
     */
    @Query("""
        SELECT AVG(fuelConsumption) 
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1 AND fuelConsumption > 0
    """)
    suspend fun getAvgConsumptionInRange(startDate: Long): Double?

    /**
     * 获取近N个月的每百公里平均价格
     */
    @Query("""
        SELECT AVG(fuelConsumption * pricePerLiter) 
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1 AND fuelConsumption > 0
    """)
    suspend fun getAvgCostPer100kmInRange(startDate: Long): Double?

    /**
     * 获取近N个月总里程（最后里程 - 最早里程）
     */
    @Query("""
        SELECT 
            CASE 
                WHEN COUNT(*) >= 2 THEN 
                    (SELECT mileage FROM fuel_records WHERE date >= :startDate ORDER BY date DESC LIMIT 1) - 
                    (SELECT mileage FROM fuel_records WHERE date >= :startDate ORDER BY date ASC LIMIT 1)
                ELSE 0.0 
            END 
        FROM fuel_records
        WHERE date >= :startDate
    """)
    suspend fun getTotalDistanceInRange(startDate: Long): Double

    /**
     * 获取近N个月总加油量
     */
    @Query("SELECT COALESCE(SUM(fuelAmount), 0) FROM fuel_records WHERE date >= :startDate")
    suspend fun getTotalFuelAmountInRange(startDate: Long): Double

    /**
     * 获取近N个月总花费
     */
    @Query("SELECT COALESCE(SUM(totalCost), 0) FROM fuel_records WHERE date >= :startDate")
    suspend fun getTotalCostInRange(startDate: Long): Double

    /**
     * 获取近N个月平均油价
     */
    @Query("SELECT AVG(pricePerLiter) FROM fuel_records WHERE date >= :startDate")
    suspend fun getAvgPriceInRange(startDate: Long): Double?

    // ==================== 按每次加油记录的查询（用于曲线图）====================

    /**
     * 获取指定时间范围内的加油记录（用于油价曲线）
     */
    @Query("""
        SELECT date, pricePerLiter, mileage
        FROM fuel_records 
        WHERE date >= :startDate 
        ORDER BY mileage ASC
    """)
    suspend fun getPriceRecords(startDate: Long): List<PriceRecordData>

    /**
     * 获取指定时间范围内有油耗的记录（用于百公里花费曲线）
     */
    @Query("""
        SELECT date, fuelConsumption, pricePerLiter, mileage
        FROM fuel_records 
        WHERE date >= :startDate AND isFull = 1 AND fuelConsumption > 0
        ORDER BY mileage ASC
    """)
    suspend fun getConsumptionRecords(startDate: Long): List<ConsumptionRecordData>
}

// ==================== 月度统计数据类 ====================

data class MonthlyFuelData(
    val month: String,
    val totalFuel: Double
)

data class MonthlyCostData(
    val month: String,
    val totalCost: Double
)

data class MonthlyConsumptionData(
    val month: String,
    val avgConsumption: Double
)

data class MonthlyCostPer100kmData(
    val month: String,
    val avgCostPer100km: Double
)

/**
 * 单次加油价格记录（用于曲线图）
 */
data class PriceRecordData(
    val date: Long,
    val pricePerLiter: Double,
    val mileage: Double
)

/**
 * 单次油耗记录（用于曲线图）
 */
data class ConsumptionRecordData(
    val date: Long,
    val fuelConsumption: Double,
    val pricePerLiter: Double,
    val mileage: Double
)
