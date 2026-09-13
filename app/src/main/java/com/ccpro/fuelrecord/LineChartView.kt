package com.ccpro.fuelrecord

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.text.DecimalFormat

/**
 * 简易折线图视图
 * - 折线使用全部数据点绘制
 * - 标签根据宽度采样显示，避免重叠
 * - 点击数据点弹出详情提示
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

    // 选中点的标记画笔
    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var dataPoints: List<ChartPoint> = emptyList()
    private val decimalFormat = DecimalFormat("#.##")

    // 采样后需要显示标签的点在原始数据中的索引
    private var labelIndexes: List<Int> = emptyList()

    // 图表面板参数（在 onDraw 中计算，在 onTouchEvent 中使用）
    private var padding = 0f
    private var chartWidth = 0f
    private var chartHeight = 0f
    private var minVal = 0.0
    private var maxVal = 0.0
    private var effectiveRange = 1.0

    // stepX：每个数据点在X轴上的像素间距
    private var stepX = 0f

    // 当前选中的点索引
    private var selectedIndex = -1

    fun setData(points: List<ChartPoint>, color: Int = Color.parseColor("#FF9800")) {
        dataPoints = points
        linePaint.color = color
        pointPaint.color = color
        selectedIndex = -1
        invalidate()
    }

    /**
     * 对原始数据点采样，返回需要显示标签的索引列表
     * 策略：固定步长采样，确保每两个标签间的索引差完全一致
     */
    private fun sampleLabelPoints(points: List<ChartPoint>, chartWidth: Float): List<Int> {
        if (points.size <= 1) return points.indices.toList()

        // 测量最长标签的宽度
        var maxWidth = 0f
        for (point in points) {
            maxWidth = kotlin.math.max(maxWidth, labelPaint.measureText(point.label))
            maxWidth = kotlin.math.max(maxWidth, valuePaint.measureText(decimalFormat.format(point.value)))
        }

        val minPointWidth = maxWidth + 10f
        val totalSpan = points.size - 1

        // 根据可用宽度计算最多能显示多少个标签
        val maxCount = kotlin.math.max(2, (chartWidth / minPointWidth).toInt() + 1)
        if (maxCount >= points.size) return points.indices.toList()

        // 从最大可能标签数向下尝试，找合理的 stride
        for (count in maxCount downTo 2) {
            val intervals = count - 1
            val strideExact = totalSpan.toDouble() / intervals
            val stride = kotlin.math.ceil(strideExact).toInt()
            if (stride < 1) continue

            // 固定 stride 生成中间点，首尾固定，避免越界
            val indexes = mutableListOf<Int>()
            indexes.add(0)
            var idx = stride
            while (idx < totalSpan) {
                indexes.add(idx)
                idx += stride
            }
            if (indexes.last() != totalSpan) {
                indexes.add(totalSpan)
            }

            // 去重
            val unique = indexes.distinct()

            // 验证：相邻间距差异不超过1
            var ok = true
            for (j in 1 until unique.size) {
                val diff = unique[j] - unique[j - 1]
                if (kotlin.math.abs(diff - strideExact) > 1.5) {
                    ok = false
                    break
                }
            }

            if (ok && unique.size >= 2) return unique
        }

        return listOf(0, totalSpan)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        padding = 65f
        chartWidth = width - padding * 2
        chartHeight = height - padding * 2

        // 全部数据点计算 min/max
        val values = dataPoints.map { it.value }
        maxVal = values.maxOrNull() ?: 0.0
        minVal = values.minOrNull() ?: 0.0
        effectiveRange = if ((maxVal - minVal) > 0) (maxVal - minVal) else 1.0

        // stepX 基于全部数据点数量计算
        stepX = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth / 2f

        // 采样用于标签显示的点
        labelIndexes = sampleLabelPoints(dataPoints, chartWidth)

        // 绘制网格线
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = padding + chartHeight * (i.toFloat() / gridLines)
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
        }

        // 绘制折线（使用全部数据点）
        val path = Path()
        dataPoints.forEachIndexed { index, point ->
            val x = padding + index * stepX
            val y = padding + chartHeight * (1 - ((point.value - minVal) / effectiveRange).toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, linePaint)

        // 只在采样点绘制小圆点（数据点太多时跳过，避免密集圆点）
        if (dataPoints.size <= 30) {
            dataPoints.forEachIndexed { index, point ->
                val x = padding + index * stepX
                val normalizedValue = (point.value - minVal) / effectiveRange
                val y = padding + chartHeight * (1 - normalizedValue.toFloat())
                canvas.drawCircle(x, y, 5f, pointPaint)
            }
        }

        // 为需要显示标签的点绘制大圆点和标签
        labelIndexes.forEach { index ->
            val point = dataPoints[index]
            val x = padding + index * stepX
            val normalizedValue = (point.value - minVal) / effectiveRange
            val y = padding + chartHeight * (1 - normalizedValue.toFloat())

            // 数据点（大）
            canvas.drawCircle(x, y, 10f, pointPaint)

            // 数值标签
            canvas.drawText(decimalFormat.format(point.value), x, y - 20f, valuePaint)

            // X轴标签
            canvas.drawText(point.label, x, height - 10f, labelPaint)
        }

        // 绘制选中点标记
        if (selectedIndex in dataPoints.indices) {
            val point = dataPoints[selectedIndex]
            val x = padding + selectedIndex * stepX
            val normalizedValue = (point.value - minVal) / effectiveRange
            val y = padding + chartHeight * (1 - normalizedValue.toFloat())

            // 外圈
            selectedPointPaint.color = Color.WHITE
            canvas.drawCircle(x, y, 16f, selectedPointPaint)
            selectedPointPaint.color = pointPaint.color
            canvas.drawCircle(x, y, 14f, selectedPointPaint)

            // 垂直虚线
            val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = Color.parseColor("#BDBDBD")
                pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
            canvas.drawLine(x, padding, x, padding + chartHeight, dashPaint)
        }

        // 绘制 Tooltip
        drawTooltip(canvas)
    }

    private var downX = 0f
    private var downY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (dataPoints.isEmpty()) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val dx = kotlin.math.abs(event.x - downX)
                val dy = kotlin.math.abs(event.y - downY)
                // 只有轻触（位移小）才认为是点击
                if (dx < 10f && dy < 10f) {
                    val touchX = event.x
                    val relativeIndex = (touchX - padding) / stepX
                    val nearestIndex = kotlin.math.round(relativeIndex).toInt().coerceIn(0, dataPoints.size - 1)
                    selectedIndex = nearestIndex
                    invalidate()
                } else {
                    selectedIndex = -1
                    invalidate()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                selectedIndex = -1
                invalidate()
            }
        }
        return true
    }

    private fun drawTooltip(canvas: Canvas) {
        if (selectedIndex < 0 || selectedIndex >= dataPoints.size) return

        val point = dataPoints[selectedIndex]
        val x = padding + selectedIndex * stepX
        val normalizedValue = (point.value - minVal) / effectiveRange
        val y = padding + chartHeight * (1 - normalizedValue.toFloat())

        val text1 = "📅 ${point.label}"
        val text2 = "💰 ${decimalFormat.format(point.value)}"

        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 40f
        }

        val textWidth1 = kotlin.math.max(centerPaint.measureText(text1), centerPaint.measureText(text2))
        val pw = (textWidth1 + 32).toInt()
        val ph = 115

        val px = (x - pw / 2f).coerceIn(0f, (width - pw).toFloat())
        val py = (y - ph - 30).coerceAtLeast(padding)

        // 背景圆角矩形
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            alpha = 220
        }
        val rRect = android.graphics.RectF(px, py, px + pw, py + ph)
        canvas.drawRoundRect(rRect, 8f, 8f, bgPaint)

        // 文字垂直居中：框高80，两行间距均匀，中心线在 py+40
        centerPaint.textSize = 32f
        canvas.drawText(text1, px + pw / 2f, py + 45f, centerPaint)
        centerPaint.textSize = 36f
        canvas.drawText(text2, px + pw / 2f, py + 90f, centerPaint)
    }
}
