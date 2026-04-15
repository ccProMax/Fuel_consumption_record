package com.ccpro.fuelrecord

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.text.DecimalFormat

/**
 * 简易折线图视图
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#FF9800")
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF9800")
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        color = Color.parseColor("#757575")
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 38f
        color = Color.parseColor("#212121")
        textAlign = Paint.Align.CENTER
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#E0E0E0")
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private var dataPoints: List<ChartPoint> = emptyList()
    private val decimalFormat = DecimalFormat("#.##")

    fun setData(points: List<ChartPoint>, color: Int = Color.parseColor("#FF9800")) {
        dataPoints = points
        linePaint.color = color
        pointPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val padding = 65f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        val values = dataPoints.map { it.value }
        val maxVal = values.maxOrNull() ?: 0.0
        val minVal = values.minOrNull() ?: 0.0
        val range = maxVal - minVal
        val effectiveRange = if (range > 0) range else 1.0

        val stepX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth / 2f

        // 绘制网格线
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + chartHeight * (i.toFloat() / gridLines)
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }

        // 绘制折线
        val path = Path()
        dataPoints.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val normalizedValue = (point.value - minVal) / effectiveRange
            val y = padding + chartHeight * (1 - normalizedValue.toFloat())

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        canvas.drawPath(path, linePaint)

        // 计算标签间隔,避免重叠
        val labelInterval = calculateLabelInterval(dataPoints.size, stepX)

        // 绘制数据点和标签
        dataPoints.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val normalizedValue = (point.value - minVal) / effectiveRange
            val y = padding + chartHeight * (1 - normalizedValue.toFloat())

            // 数据点
            canvas.drawCircle(x, y, 10f, pointPaint)

            // 数值标签(根据间隔显示)
            if (index % labelInterval == 0) {
                canvas.drawText(decimalFormat.format(point.value), x, y - 20f, valuePaint)
            }

            // X轴标签(根据间隔显示)
            if (index % labelInterval == 0) {
                canvas.drawText(point.label, x, height - 10f, labelPaint)
            }
        }
    }

    /**
     * 计算标签间隔,避免重叠
     * @param dataCount 数据点数量
     * @param stepX X轴步长(像素)
     * @return 标签间隔(每隔几个点显示一个标签)
     */
    private fun calculateLabelInterval(dataCount: Int, stepX: Float): Int {
        if (dataCount <= 7) return 1  // 7个或更少的数据点,全部显示

        // 估算标签宽度(假设日期标签大约需要60dp宽度)
        val labelWidthDp = 60f
        val density = resources.displayMetrics.density
        val labelWidthPx = labelWidthDp * density

        // 计算需要多少个间隔才能避免重叠
        val minInterval = (labelWidthPx / stepX).toInt()

        // 返回合适的间隔,至少为1,最大不超过数据点数的一半
        return maxOf(1, kotlin.math.min(minInterval, dataCount / 2))
    }
}
