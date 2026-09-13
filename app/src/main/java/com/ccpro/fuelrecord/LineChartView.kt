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

    /**
     * 根据标签宽度采样数据点,避免重叠
     * 采样时会同时考虑日期标签和数值标签的宽度
     */
    private fun sampleDataPoints(points: List<ChartPoint>, chartWidth: Float): List<ChartPoint> {
        if (points.size <= 1) return points

        // 测量最长日期标签的宽度
        var maxLabelWidth = 0f
        for (point in points) {
            val labelWidth = labelPaint.measureText(point.label)
            if (labelWidth > maxLabelWidth) maxLabelWidth = labelWidth
        }

        // 测量最长数值标签的宽度
        var maxValueWidth = 0f
        for (point in points) {
            val valueStr = decimalFormat.format(point.value)
            val valueWidth = valuePaint.measureText(valueStr)
            if (valueWidth > maxValueWidth) maxValueWidth = valueWidth
        }

        // 取两者中的较大值作为每个点所需的最小宽度
        // 因为标签居中显示,两边各有一半,所以需要的间距是标签宽度的完整宽度
        val minPointWidth = maxOf(maxLabelWidth, maxValueWidth)

        // 计算最多可以显示多少个点
        val maxPoints = kotlin.math.max(2, (chartWidth / minPointWidth).toInt())

        if (points.size <= maxPoints) return points

        // 均匀采样,始终保留第一个和最后一个点
        val step = (points.size - 1).toDouble() / (maxPoints - 1)
        val sampled = mutableListOf<ChartPoint>()

        for (i in 0 until maxPoints) {
            val index = Math.round(i * step).toInt().coerceIn(0, points.size - 1)
            sampled.add(points[index])
        }

        return sampled
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val padding = 65f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // 根据标签宽度采样数据点
        val displayPoints = sampleDataPoints(dataPoints, chartWidth)

        val values = displayPoints.map { it.value }
        val maxVal = values.maxOrNull() ?: 0.0
        val minVal = values.minOrNull() ?: 0.0
        val range = maxVal - minVal
        val effectiveRange = if (range > 0) range else 1.0

        val stepX = if (displayPoints.size > 1) chartWidth / (displayPoints.size - 1) else chartWidth / 2f

        // 绘制网格线
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + chartHeight * (i.toFloat() / gridLines)
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }

        // 绘制折线
        val path = Path()
        displayPoints.forEachIndexed { index, point ->
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

        // 绘制数据点和标签（采样后point间距已足够，无需额外间隔）
        displayPoints.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val normalizedValue = (point.value - minVal) / effectiveRange
            val y = padding + chartHeight * (1 - normalizedValue.toFloat())

            // 数据点
            canvas.drawCircle(x, y, 10f, pointPaint)

            // 数值标签
            canvas.drawText(decimalFormat.format(point.value), x, y - 20f, valuePaint)

            // X轴标签
            canvas.drawText(point.label, x, height - 10f, labelPaint)
        }
    }
}
