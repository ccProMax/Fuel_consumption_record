package com.example.fuelrecord

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 油耗记录数据模型
 * 包含一次加油记录的所有信息
 */
@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 加油日期
    val date: Date = Date(),

    // 当前里程（公里）
    val mileage: Double,

    // 加油量（升）
    val fuelAmount: Double,

    // 单价（元/升）
    val pricePerLiter: Double,

    // 总价（元）
    val totalCost: Double = 0.0,

    // 备注
    val note: String = "",

    // 油耗（升/百公里）
    val fuelConsumption: Double = 0.0,

    // 汽油标号（如 92, 95, 98）
    val fuelType: Int = 92,

    // 是否加满
    val isFull: Boolean = true
) {
    /**
     * 计算总价
     */
    fun calculateTotalCost(): Double {
        return fuelAmount * pricePerLiter
    }

    /**
     * 获取格式化日期字符串
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * 获取格式化月份字符串
     */
    fun getMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * 获取汽油标号字符串
     */
    fun getFuelTypeLabel(): String = "#$fuelType"
}

/**
 * 带有行驶里程的扩展记录（用于列表显示）
 */
data class FuelRecordWithDistance(
    val record: FuelRecord,
    val distanceAdded: Double = 0.0,
    val hasPreviousRecord: Boolean = false
)

/**
 * 油耗统计信息
 */
data class FuelStatistics(
    val totalRecords: Int = 0,
    val totalFuelAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val totalDistance: Double = 0.0,
    val averageConsumption: Double = 0.0,
    val averagePrice: Double = 0.0,
    val latestConsumption: Double = 0.0,
    val consumptionTrend: Double = 0.0
)

/**
 * 看板周期性统计信息（受时间周期影响）
 */
data class DashboardPeriodStats(
    val periodLabel: String,
    val avgConsumption: Double = 0.0,
    val avgPricePerKm: Double = 0.0
)

/**
 * 看板全局统计信息（不受时间周期影响）
 */
data class DashboardGlobalStats(
    val totalDistance: Double = 0.0,
    val totalFuelAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val avgPricePerLiter: Double = 0.0
)

/**
 * 看板统计信息（按时间周期）- 已废弃，保留用于兼容
 */
@Deprecated("使用 DashboardPeriodStats 和 DashboardGlobalStats 替代")
data class DashboardStats(
    val periodLabel: String,
    val avgConsumption: Double = 0.0,
    val avgPricePer100km: Double = 0.0,
    val totalDistance: Double = 0.0,
    val totalFuelAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val avgPricePerLiter: Double = 0.0
)

/**
 * 图表数据点
 */
data class ChartPoint(
    val label: String,
    val value: Double
)
