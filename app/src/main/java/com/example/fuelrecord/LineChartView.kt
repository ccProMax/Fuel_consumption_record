package com.example.fuelrecord

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
        strokeWidth = 3f
        color = Color.parseColor("#FF9800")
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FF9800")
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.parseColor("#757575")
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
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

        val padding = 50f
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

        // 绘制数据点和标签
        dataPoints.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val normalizedValue = (point.value - minVal) / effectiveRange
            val y = padding + chartHeight * (1 - normalizedValue.toFloat())

            // 数据点
            canvas.drawCircle(x, y, 6f, pointPaint)

            // 数值标签
            canvas.drawText(decimalFormat.format(point.value), x, y - 15f, valuePaint)

            // X轴标签
            canvas.drawText(point.label, x, height - 10f, labelPaint)
        }
    }
}
