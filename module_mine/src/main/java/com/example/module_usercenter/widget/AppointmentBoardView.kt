package com.example.module_usercenter.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withClip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class Appointment(
    val id: Int,
    var dayOffset: Int, // 0..N
    var period: Int, // 0 上午 1 下午
    val name: String,
    var color: Int
)

/**
 * 1.onDraw中的绘制 标头 左侧固定列 表格 每一项item
 * 2.双指缩放 通过ScaleGestureDetector计算缩放因子，然后计算col宽度/显示日数量，offsetX的偏移量
 * 3.横向滑动 GestureDetector捕获滚动事件 OverScroller和 onFling 实现惯性滚动 计算offsetX偏移量重新绘制左边界
 * 4.拖拽item 捕获按下位置-寻找对应item-移动绘制阴影/绘制placeholder-放下
 */
@RequiresApi(Build.VERSION_CODES.O)
class AppointmentBoardView(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), GestureDetector.OnGestureListener {

    //预约集合
    private val appointments = mutableListOf<Appointment>()

    //整个内容区域（日期、网格和日程）相对于 View 可见区域向右移动的距离（即内容向左滚动的距离）
    private var offsetX = 0f

    //表格默认宽度
    private var colWidth = 0f

    // 单个列默认宽度
    private var rowHeight = 0f

    // header区域默认高度
    private var headerHeight = 80f

    //左侧固定列的宽度
    private var leftColumnWidth = 120f

    //最少最多日期 默认显示日期
    private val minDays = 2
    private val maxDays = 10
    private var daysShown = 3
    private val maxPerCell = 8
    private var daysShownFloat = daysShown.toFloat()

    //是否正在缩放中
    private var onScaling = false

    //日期相关
    private var today = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

    //缩放、滑动、手势相关
    private val scroller = OverScroller(context)
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, this)

    // 拖拽相关
    private var isDragging = false
    private var draggedAppointment: Appointment? = null
    private var dragStartX = 0f // 当前拖拽的 X 坐标
    private var dragStartY = 0f // 当前拖拽的 Y 坐标
    private var dragOffsetX = 0f // 拖拽Item的中心点和手指点击点的偏移量 X
    private var dragOffsetY = 0f // 拖拽Item的中心点和手指点击点的偏移量 Y
    private var dragCurrentX = 0f // 当前拖拽的 X 坐标
    private var dragCurrentY = 0f // 当前拖拽的 Y 坐标

    // 新增：占位符位置信息
    private var placeholderCell: Pair<Int, Int>? = null // 占位符所在单元格 (dayOffset, period)
    private var placeholderIndex: Int? = null // 占位符在目标列表中的索引

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88AAAAAA.toInt() // 半透明灰色
        style = Paint.Style.FILL
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        setShadowLayer(10f, 5f, 5f, 0xAA000000.toInt()) // 阴影效果
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        textSize = 34f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    init {
        val colors = listOf("#42A5F5", "#FF7043", "#AB47BC", "#7CB342")
        var id = 1
        for (day in 0 until 10) {
            for (i in 0 until (3..10).random()) {
                appointments.add(
                    Appointment(
                        id++,
                        day,
                        if (i % 2 == 0) 0 else 1,
                        "任务$id",
                        colors.random().toColorInt()
                    )
                )
            }
        }
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 根据屏幕大小重新计算 每个cell的宽度和高度
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateColWidth()
        rowHeight = (h - headerHeight) / 2
        val maxX = reCalculateMaxX()
        offsetX = offsetX.coerceIn(0f, maxX)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //画左侧的上午下午固定表格
        drawLeftFixedColumn(canvas)
        //画标头 日期
        drawHeader(canvas)
        //画每天的表格背景
        drawGrid(canvas)
        //画每一项item
        drawAppointments(canvas)
    }

    private fun drawAppointments(canvas: Canvas) {
        val leftStart = leftColumnWidth - offsetX
        val itemH = rowHeight / maxPerCell

        // 1. 遍历并绘制所有日程（包括占位符和排除被拖动的日程）
        // 把集合 根据 上午下午分组 类似同一天的上午有几个list 下午有几个list
        appointments.groupBy { it.dayOffset to it.period }.forEach { (cell, allList) ->
            val day = cell.first
            val period = cell.second

            // 过滤：获取当前 Cell 中“非拖动中”的日程列表
            val list = allList.filter { it != draggedAppointment }

            // 同一天的左侧都一样
            val baseLeft = leftStart + day * colWidth
            // 上午和下午的组 头部基本线
            val baseTop = headerHeight + period * rowHeight

            // 优化：如果整个 Cell 不在可见区域，则跳过
            if (baseLeft + colWidth < leftColumnWidth || baseLeft > width) return@forEach

            // 占位符绘制逻辑：
            val isPlaceholderCell = cell == placeholderCell && placeholderIndex != null
            var currentDrawIndex = 0 // 当前 Cell 的绘制索引，用于处理占位符引起的位移

            // 遍历所有非拖动中的日程
            list.forEachIndexed { index, appointment ->

                // 检查是否需要在当前索引前绘制占位符
                if (isPlaceholderCell && currentDrawIndex == placeholderIndex) {
                    // 绘制占位符 (Placeholder)
                    val placeholderTop = baseTop + currentDrawIndex * itemH
                    val placeholderRect = RectF(
                        max(baseLeft, leftColumnWidth),
                        placeholderTop,
                        baseLeft + colWidth,
                        placeholderTop + itemH
                    )
                    canvas.drawRoundRect(placeholderRect, 12f, 12f, placeholderPaint)
                    currentDrawIndex++ // 占位符占据一个位置
                }

                // 绘制当前日程
                val top = baseTop + currentDrawIndex * itemH
                val rect = RectF(max(baseLeft, leftColumnWidth), top, baseLeft + colWidth, top + itemH)

                paint.color = appointment.color
                canvas.drawRoundRect(rect, 12f, 12f, paint)
                canvas.drawText(appointment.name, rect.left + 5f, rect.centerY() + 10f, textPaint)

                currentDrawIndex++
            }

            // 循环结束后，如果占位符是 Cell 最后一个，则在这里绘制
            if (isPlaceholderCell && currentDrawIndex == placeholderIndex) {
                val placeholderTop = baseTop + currentDrawIndex * itemH
                val placeholderRect = RectF(
                    max(baseLeft, leftColumnWidth),
                    placeholderTop,
                    baseLeft + colWidth,
                    placeholderTop + itemH
                )
                canvas.drawRoundRect(placeholderRect, 12f, 12f, placeholderPaint)
            }
        }

        // 2. 绘制被拖动的日程（带阴影），使其浮动在最上层
        draggedAppointment?.let { appt ->
            val itemHForDrag = rowHeight / maxPerCell

            // 计算拖动日程的绘制矩形
            val rect = RectF(
                dragCurrentX + dragOffsetX - colWidth / 2, // 使用 colWidth 作为拖动时的宽度
                dragCurrentY + dragOffsetY - itemHForDrag / 2,
                dragCurrentX + dragOffsetX + colWidth / 2,
                dragCurrentY + dragOffsetY + itemHForDrag / 2
            )

            // 使用 shadowPaint 绘制阴影（在软件层面上有效）
            canvas.drawRoundRect(rect, 12f, 12f, shadowPaint)

            // 绘制日程本体
            paint.color = appt.color
            canvas.drawRoundRect(rect, 12f, 12f, paint)

            // 绘制文字
            canvas.drawText(appt.name, rect.left + 5f, rect.centerY() + 10f, textPaint)
        }
    }

    private fun drawGrid(canvas: Canvas) {
        val leftStart = leftColumnWidth - offsetX
        for (col in 0 until maxDays) {
            val left = leftStart + col * colWidth
            val right = left + colWidth
            // 优化：仅绘制可见或部分可见的列
            if (right < leftColumnWidth || left > width) continue
            paint.color = if (col % 2 == 0) 0xFFF9F9F9.toInt() else 0xFFFFFFFF.toInt()

            canvas.drawRect(
                max(left, leftColumnWidth),
                headerHeight,
                right,
                headerHeight + rowHeight * 2,
                paint
            )
            paint.color = 0xFFCCCCCC.toInt()
            canvas.drawLine(
                max(left, leftColumnWidth),
                headerHeight + rowHeight,
                right,
                headerHeight + rowHeight,
                paint
            )
            canvas.drawLine(right, headerHeight, right, headerHeight + rowHeight * 2, paint)
        }

    }

    private fun drawHeader(canvas: Canvas) {
        paint.color = "#eeeeee".toColorInt()
        canvas.drawRect(leftColumnWidth, 0f, width.toFloat(), headerHeight, paint)
        //裁剪canvas防止文字绘制超出此区域 侵占到左侧固定列
        canvas.withClip(leftColumnWidth, 0f, width.toFloat(), headerHeight) {
            val leftStart = leftColumnWidth - offsetX
            for (col in 0 until maxDays) {
                val left = leftStart + col * colWidth
                val centerX = left + colWidth / 2
                val date = today.plusDays(col.toLong())
                val dateText = date.format(dateFormatter)
                drawText(dateText, centerX, 50f, headerPaint)
            }
        }
    }

    private fun drawLeftFixedColumn(canvas: Canvas) {
        paint.color = "#F2f2f2".toColorInt()
        canvas.drawRect(0f, headerHeight, leftColumnWidth, height.toFloat(), paint)

        paint.color = Color.GRAY
        paint.textSize = 36f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("上午", leftColumnWidth / 2, rowHeight / 2, paint)
        canvas.drawText("下午", leftColumnWidth / 2, (rowHeight * 1.5).toFloat(), paint)
        canvas.drawLine(
            0f,
            headerHeight + rowHeight,
            leftColumnWidth,
            headerHeight + rowHeight,
            paint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        draggedAppointment?.let {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    // 1. 更新手指的当前位置
                    dragCurrentX = event.x
                    dragCurrentY = event.y

                    // 2. 移除重复的 dragOffsetX/Y 计算（它们应在 onLongPress 中固定）
                    // 确保 dragOffsetX/Y 保持不变，代表点击点到 Item 中心的偏移

                    // 3. 更新占位符位置（注意第二个参数是 event.y）
                    updatePlaceholder(event.x, event.y)

                    invalidate()
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // ... 放置和重置逻辑不变
                    handleDrop()
                    draggedAppointment = null
                    isDragging = false
                    placeholderCell = null
                    placeholderIndex = null
                    invalidate()
                    return true
                }

                else -> {}
            }
        }
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var lastScaleTime = 0L

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastScaleTime < 10) return true
            onScaling = true
            val oldColWidth = colWidth
            var factor = detector.scaleFactor
            factor = 1 + (factor - 1) * 0.5f // 减缓缩放速度
            //反比例 factor越大 可显示日期越少
            daysShownFloat =
                (daysShownFloat / factor).coerceIn(minDays.toFloat(), maxDays.toFloat())
            updateColWidth() // 更新 colWidth

            // 计算缩放导致的 offsetX 变化（保持焦点不变）
            val focusX = detector.focusX
            offsetX =
                ((offsetX + focusX - leftColumnWidth) * (colWidth / oldColWidth) - (focusX - leftColumnWidth))
            // 约束 offsetX
            val maxX = reCalculateMaxX()
            offsetX = offsetX.coerceIn(0f, maxX)

            postInvalidateOnAnimation()
            lastScaleTime = now
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            // 最终约束 daysShown 为整数
            daysShown = daysShownFloat.roundToInt().coerceIn(minDays, maxDays)
            daysShownFloat = daysShown.toFloat()
            updateColWidth()
            // 约束最终的 offsetX
            val maxX = reCalculateMaxX()
            offsetX = offsetX.coerceIn(0f, maxX)
            onScaling = false
        }
    }

    override fun onDown(e: MotionEvent): Boolean = true

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean = true

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!onScaling) {
            Log.d("99788", "onScroll:$distanceX--$distanceY")
            val maxX = reCalculateMaxX()
            // 2. 更新 offsetX，并约束其范围
            offsetX = (offsetX + distanceX).coerceIn(0f, maxX)
            postInvalidateOnAnimation()
        }
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (onScaling) return
        // 找到长按的item
        draggedAppointment = findAppointmentAt(e.x, e.y)
        draggedAppointment?.let {
            isDragging = true
            dragStartX = e.x
            dragStartY = e.y
            dragOffsetX = 0f
            dragOffsetY = 0f
            invalidate()
        }
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        val maxX = reCalculateMaxX()
        // 启动 OverScroller 计算惯性数据
        scroller.fling(
            offsetX.roundToInt(),
            0,
            (-velocityX).roundToInt(), // 注意：惯性速度需要与坐标系方向相反
            0,
            0,
            maxX.roundToInt(),
            0,
            0
        )
        //启动惯性滚动的动画循环 第一帧
        postInvalidateOnAnimation()
        return true
    }

    /**
     * 根据当前手指位置 (x, y) 实时计算并更新占位符的位置 (placeholderCell, placeholderIndex)
     */
    private fun updatePlaceholder(x: Float, y: Float) {
        val leftStart = leftColumnWidth - offsetX
        // 1. 计算新的单元格
        val relX = x - leftStart
        val newDay = floor(relX / colWidth).toInt().coerceIn(0, maxDays - 1)
        val newPeriod = floor((y - headerHeight) / rowHeight).toInt().coerceIn(0, 1)

        // 如果手指在左侧固定列或标题栏，则不处理占位符（保持上一个有效位置）
        if (x < leftColumnWidth || y < headerHeight) return

        placeholderCell = newDay to newPeriod

        // 2. 计算新 Cell 内的插入索引
        val baseTop = headerHeight + newPeriod * rowHeight
        val itemH = rowHeight / maxPerCell

        // 计算 Cell 内的相对 Y 坐标
        val relYInCell = y - baseTop

        // 向上取整计算索引（因为 itemH 是高度，每经过一个 itemH 区域，索引就+1）
        // 这里的 floor(relYInCell / itemH) 得到的是经过了多少个完整 item 高度
        var newIndex = floor(relYInCell / itemH).toInt()

        // 约束索引：不能超过该 Cell 中已有日程数量（除了被拖动的那一个）
        val cellAppointmentsCount = appointments.count {
            it.dayOffset == newDay && it.period == newPeriod && it != draggedAppointment
        }

        placeholderIndex = newIndex.coerceIn(0, cellAppointmentsCount)
    }

    /**
     * 1. 在原来的appointments日期列表下删除
     * 2. 在x,y所在的日期列表下对应的位置插入修改list
     * 3. 替换原来的appointments 并重新绘制
     */
    /**
     * 放置日程。根据占位符的位置更新数据列表。
     */
    private fun handleDrop() {
        draggedAppointment?.let { appt ->
            val targetCell = placeholderCell
            val targetIndex = placeholderIndex

            // 只有当有有效的放置位置时才进行数据修改
            if (targetCell != null && targetIndex != null) {
                val (newDay, newPeriod) = targetCell

                // 1. 从appointments集合中删除被拖动的日程
                appointments.remove(appt)

                // 2. 准备目标 Cell 的日程列表（不含被拖动的日程）
                val targetList = appointments.filter {
                    it.dayOffset == newDay && it.period == newPeriod
                }.toMutableList()

                // 3. 更新被拖动日程的 Cell 信息
                appt.dayOffset = newDay
                appt.period = newPeriod

                // 4. 在目标列表的指定索引处插入日程
                targetList.add(targetIndex, appt)

                // 5. 将更新后的目标列表替换回 appointments
                // 首先删除旧的 (newDay, newPeriod) 的所有日程
                appointments.removeAll { it.dayOffset == newDay && it.period == newPeriod }

                // 然后添加新的列表
                appointments.addAll(targetList)

                // 重新排序整个 appointments 列表（可选，如果需要保持整体有序性）
                appointments.sortBy { it.dayOffset * 100 + it.period * 50 + appointments.indexOf(it) }
            }
        }
    }

    /**
     * 在onDraw之前，帧昏眩，每一帧取出计算结果 (scroller.currX) 并持续驱动重绘
     */
    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            // 计算最大滚动偏移量
            val maxX = reCalculateMaxX()
            // 应用 scroller 计算出的新坐标，并约束
            offsetX = scroller.currX.toFloat().coerceIn(0f, maxX)
            postInvalidateOnAnimation()
        }
    }

    private fun findAppointmentAt(x: Float, y: Float): Appointment? {
        val dayIndex = ((x - leftColumnWidth) / colWidth).toInt()
        val period = if (y < headerHeight + rowHeight) 0 else 1
        val cellAppointments =
            appointments.filter { it.dayOffset == dayIndex && it.period == period }
        val baseTop = headerHeight + period * rowHeight
        val itemH = rowHeight / maxPerCell
        cellAppointments.forEachIndexed { index, appointment ->
            val top = baseTop + index * itemH
            val left = leftColumnWidth - offsetX + dayIndex * colWidth
            val rect = RectF(max(left, leftColumnWidth), top, left + colWidth, top + itemH)
            if (rect.contains(x, y)) {
                dragOffsetX = rect.centerX() - x
                dragOffsetY = rect.centerY() - y
                return appointment
            }
        }
        return null
    }

    /**
     * 更新列的宽度
     */
    private fun updateColWidth() {
        colWidth = (width - leftColumnWidth) / daysShownFloat
    }

    /**
     * 计算最大滚动偏移量
     */
    private fun reCalculateMaxX(): Float {
        val totalContentWidth = colWidth * maxDays.toFloat()
        val visibleScrollAreaWidth = width - leftColumnWidth
        return max(0f, totalContentWidth - visibleScrollAreaWidth)
    }
}