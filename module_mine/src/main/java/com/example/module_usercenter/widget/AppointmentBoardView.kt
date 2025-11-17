package com.example.module_usercenter.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.OverScroller
import android.widget.Toast
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
 * 4.拖拽item 捕获按下位置-寻找对应item/index-move事件获取移动下标-
 * 移动绘制阴影/placeholder(移动时跳过绘制原来的item，相同placeholderIndex绘制占位再绘制正常的item)-
 * up/cancel删除就的数据插入新的数据
 *
 * 🌟 增加功能:
 * 1. 当位置移动时增加一个回调 (onAppointmentMovedListener)
 * 2. 移动时增加一个拦截 (onBeforeAppointmentDropListener)
 */
@RequiresApi(Build.VERSION_CODES.O)
class AppointmentBoardView(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), GestureDetector.OnGestureListener {

    // 预约集合
    private val appointments = mutableListOf<Appointment>()

    // 🌟 新增：原始位置信息，用于拖拽取消时恢复
    private var originalDayOffset = -1
    private var originalPeriod = -1

    // 🌟 新增回调接口
    /**
     * 拖拽放下时，在数据实际变更前进行拦截。
     * @param appointment 被拖动的日程
     * @param newDayOffset 目标日期的偏移量
     * @param newPeriod 目标时段 (0:上午, 1:下午)
     * @param newIndex 目标时段内的插入索引
     * @return true: 确认修改位置，进行数据变更。false: 取消修改，日程回到原位。
     */
    var onBeforeAppointmentDropListener: ((
        appointment: Appointment,
        newDayOffset: Int,
        newPeriod: Int,
        newIndex: Int
    ) -> Boolean)? = null

    /**
     * 日程位置成功移动后触发的回调，用于业务同步服务器。
     * 仅在 onBeforeAppointmentDropListener 返回 true 后调用。
     * @param appointment 成功移动的日程
     * @param oldDayOffset 移动前的日期偏移量
     * @param oldPeriod 移动前的时段
     */
    var onAppointmentMovedListener: ((
        appointment: Appointment,
        oldDayOffset: Int,
        oldPeriod: Int
    ) -> Unit)? = null

    // 整个内容区域（日期、网格和日程）相对于 View 可见区域向右移动的距离（即内容向左滚动的距离）
    private var offsetX = 0f

    // 表格默认宽度
    private var colWidth = 0f

    // 单个列默认宽度
    private var rowHeight = 0f

    // 🌟 优化1: 转换为 px，以实现尺寸适配
    private var headerHeight = 0f // 初始值在 init 中计算
    private var leftColumnWidth = 0f // 初始值在 init 中计算
    private var itemPadding = 0f // 新增：Item 之间的内边距

    // 最少最多日期 默认显示日期
    private val minDays = 2
    private val maxDays = 10
    private var daysShown = 3
    private val maxPerCell = 8
    private var daysShownFloat = daysShown.toFloat()

    // 是否正在缩放中
    private var onScaling = false

    // 日期相关
    private var today = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

    // 缩放、滑动、手势相关
    private val scroller = OverScroller(context)
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, this)

    // 拖拽相关
    private var isDragging = false
    private var draggedAppointment: Appointment? = null

    // 🌟 优化3: dragStartX/Y 不再使用，直接用 dragCurrentX/Y 和 dragOffsetX/Y
    private var dragOffsetX = 0f // 拖拽Item的左上角和手指点击点的偏移量 X (左上角 X - 点击 X)
    private var dragOffsetY = 0f // 拖拽Item的左上角和手指点击点的偏移量 Y (左上角 Y - 点击 Y)
    private var dragCurrentX = 0f // 当前手指的 X 坐标
    private var dragCurrentY = 0f // 当前手指的 Y 坐标

    // 新增：占位符位置信息
    private var placeholderCell: Pair<Int, Int>? = null // 占位符所在单元格 (dayOffset, period)
    private var placeholderIndex: Int? = null // 占位符在目标列表中的索引

    // 🌟 优化2: 预创建 Paint 对象以提高性能
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val appointmentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 🌟 优化2: 预创建 RectF 对象以减少 onDraw 时的内存分配
    private val tempRectF = RectF()

    init {
        // 🌟 优化1: 尺寸单位转换
        headerHeight = dpToPx(context, 48f) // 48dp
        leftColumnWidth = dpToPx(context, 60f) // 60dp
        itemPadding = dpToPx(context, 2f) // 2dp

        // 初始化 Paint
        // 通用文本 Paint
        textPaint.apply {
            color = Color.WHITE
            textSize = spToPx(context, 10f) // 10sp
        }
        // 表头文本 Paint
        headerPaint.apply {
            color = Color.DKGRAY
            textSize = spToPx(context, 12f) // 12sp
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        // 占位符 Paint
        placeholderPaint.apply {
            color = 0x88AAAAAA.toInt() // 半透明灰色
            style = Paint.Style.FILL
        }
        // 阴影 Paint
        shadowPaint.apply {
            color = Color.BLACK
            setShadowLayer(
                dpToPx(context, 5f),
                dpToPx(context, 3f),
                dpToPx(context, 3f),
                0xAA000000.toInt()
            )
        }
        // 背景 Paint (不需要设置太多，用于网格/左列绘制)

        // 模拟数据生成
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
        // 开启软件层绘图以支持阴影
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // 🌟 辅助方法：将 DP 转换为像素
    private fun dpToPx(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    // 🌟 辅助方法：将 SP 转换为像素
    private fun spToPx(context: Context, sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
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

    /**
     * 1. 绘制placeholder
     * 2. 绘制正常的item
     * 3. 绘制被拖动的item
     */
    private fun drawAppointments(canvas: Canvas) {
        val leftStart = leftColumnWidth - offsetX
        val itemH = rowHeight / maxPerCell
        val itemDrawH = itemH - itemPadding * 2

        // 🌟 优化：在绘制所有可滚动的日程之前，先对 Canvas 进行裁剪
        canvas.withClip(leftColumnWidth, headerHeight, width.toFloat(), height.toFloat()) {

            // 1. 遍历并绘制所有日程（包括占位符和排除被拖动的日程）
            // 注意：groupBy 每次拖拽都会重建，但目前性能尚可，可接受
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
                var currentDrawIndex = 0

                // 遍历所有非拖动中的日程
                list.forEachIndexed { _, appointment ->
                    // 检查是否需要在当前索引前绘制占位符
                    if (isPlaceholderCell && currentDrawIndex == placeholderIndex) {
                        drawPlaceholder(
                            canvas,
                            baseLeft,
                            baseTop,
                            colWidth,
                            itemH,
                            itemDrawH,
                            currentDrawIndex
                        )
                        currentDrawIndex++ // 占位符占据一个位置
                    }

                    // 绘制当前日程
                    drawSingleAppointment(
                        canvas,
                        appointment,
                        baseLeft,
                        baseTop,
                        colWidth,
                        itemH,
                        itemDrawH,
                        currentDrawIndex
                    )
                    currentDrawIndex++
                }

                // 循环结束后，如果占位符是 Cell 最后一个，则在这里绘制
                if (isPlaceholderCell && currentDrawIndex == placeholderIndex) {
                    drawPlaceholder(
                        canvas,
                        baseLeft,
                        baseTop,
                        colWidth,
                        itemH,
                        itemDrawH,
                        currentDrawIndex
                    )
                }
            }
        } // 裁剪结束

        // 2. 绘制被拖动的日程（带阴影），使其浮动在最上层
        // 拖拽 Item 应该浮动在整个 View 上方，不应该被裁剪
        draggedAppointment?.let { appt ->
            // 🌟 优化3: 直接使用 dragCurrentX/Y 和 dragOffsetX/Y 计算 Item 左上角
            val left = dragCurrentX + dragOffsetX
            val top = dragCurrentY + dragOffsetY

            // 🌟 优化2: 使用 tempRectF
            tempRectF.set(
                left,
                top,
                left + colWidth,
                top + itemDrawH // 拖拽时Item的高度保持一致
            )

            // 使用 shadowPaint 绘制阴影（在软件层面上有效）
            canvas.drawRoundRect(tempRectF, dpToPx(context, 4f), dpToPx(context, 4f), shadowPaint)

            // 绘制日程本体
            appointmentPaint.color = appt.color
            canvas.drawRoundRect(
                tempRectF,
                dpToPx(context, 4f),
                dpToPx(context, 4f),
                appointmentPaint
            )

            // 绘制文字
            canvas.drawText(
                appt.name,
                tempRectF.left + itemPadding * 2,
                tempRectF.centerY() + textPaint.textSize / 3,
                textPaint
            )
        }
    }

    private fun drawPlaceholder(
        canvas: Canvas,
        baseLeft: Float,
        baseTop: Float,
        colWidth: Float,
        itemH: Float,
        itemDrawH: Float,
        index: Int
    ) {
        val placeholderTop = baseTop + index * itemH + itemPadding
        tempRectF.set(
            baseLeft + itemPadding,
            placeholderTop,
            baseLeft + colWidth - itemPadding,
            placeholderTop + itemDrawH
        )
        canvas.drawRoundRect(tempRectF, dpToPx(context, 4f), dpToPx(context, 4f), placeholderPaint)
    }

    private fun drawSingleAppointment(
        canvas: Canvas,
        appointment: Appointment,
        baseLeft: Float,
        baseTop: Float,
        colWidth: Float,
        itemH: Float,
        itemDrawH: Float,
        index: Int
    ) {
        val top = baseTop + index * itemH + itemPadding
        val left = baseLeft + itemPadding

        // 🌟 优化2: 使用 tempRectF
        tempRectF.set(
            left,
            top,
            baseLeft + colWidth - itemPadding,
            top + itemDrawH
        )

        appointmentPaint.color = appointment.color
        canvas.drawRoundRect(tempRectF, dpToPx(context, 4f), dpToPx(context, 4f), appointmentPaint)

        // 绘制文字，稍微调整 centerY 偏移
        canvas.drawText(
            appointment.name,
            tempRectF.left + itemPadding * 2,
            tempRectF.centerY() + textPaint.textSize / 3,
            textPaint
        )
    }

    private fun drawGrid(canvas: Canvas) {
        val leftStart = leftColumnWidth - offsetX
        for (col in 0 until maxDays) {
            val left = leftStart + col * colWidth
            val right = left + colWidth
            // 优化：仅绘制可见或部分可见的列
            if (right < leftColumnWidth || left > width) continue

            // 🌟 优化2: 使用 backgroundPaint
            backgroundPaint.color = if (col % 2 == 0) 0xFFF9F9F9.toInt() else 0xFFFFFFFF.toInt()

            canvas.drawRect(
                max(left, leftColumnWidth),
                headerHeight,
                right,
                headerHeight + rowHeight * 2,
                backgroundPaint
            )

            // 绘制网格线
            gridPaint.color = 0xFFCCCCCC.toInt()
            // 中间分割线
            canvas.drawLine(
                max(left, leftColumnWidth),
                headerHeight + rowHeight,
                right,
                headerHeight + rowHeight,
                gridPaint
            )
            // 右侧分割线
            canvas.drawLine(right, headerHeight, right, headerHeight + rowHeight * 2, gridPaint)
        }

    }

    private fun drawHeader(canvas: Canvas) {
        // 绘制背景
        backgroundPaint.color = "#eeeeee".toColorInt()
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, backgroundPaint)

        // 绘制左侧固定列与可滚动区域的分隔线
        gridPaint.color = Color.GRAY
        canvas.drawLine(leftColumnWidth, 0f, leftColumnWidth, headerHeight, gridPaint)

        //裁剪canvas防止文字绘制超出此区域 侵占到左侧固定列
        canvas.withClip(leftColumnWidth, 0f, width.toFloat(), headerHeight) {
            val leftStart = leftColumnWidth - offsetX
            for (col in 0 until maxDays) {
                val left = leftStart + col * colWidth
                val centerX = left + colWidth / 2
                val date = today.plusDays(col.toLong())
                val dateText = date.format(dateFormatter)
                // 绘制日期文本
                canvas.drawText(
                    dateText,
                    centerX,
                    headerHeight / 2 + headerPaint.textSize / 3,
                    headerPaint
                )
            }
        }
    }

    private fun drawLeftFixedColumn(canvas: Canvas) {
        // 绘制背景
        backgroundPaint.color = "#F2f2f2".toColorInt()
        canvas.drawRect(0f, headerHeight, leftColumnWidth, height.toFloat(), backgroundPaint)

        // 绘制分隔线（标题栏）
        gridPaint.color = Color.GRAY
        canvas.drawLine(0f, headerHeight, leftColumnWidth, headerHeight, gridPaint)

        // 绘制文字
        headerPaint.color = Color.GRAY
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "上午",
            leftColumnWidth / 2,
            headerHeight + rowHeight / 2 + headerPaint.textSize / 3,
            headerPaint
        )
        canvas.drawText(
            "下午",
            leftColumnWidth / 2,
            headerHeight + (rowHeight * 1.5).toFloat() + headerPaint.textSize / 3,
            headerPaint
        )

        // 绘制上下午分割线
        gridPaint.color = 0xFFCCCCCC.toInt()
        canvas.drawLine(
            0f,
            headerHeight + rowHeight,
            leftColumnWidth,
            headerHeight + rowHeight,
            gridPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 先处理缩放
        scaleDetector.onTouchEvent(event)

        // 如果正在缩放，则不处理其他手势
        if (onScaling) return true

        // 拖拽处理优先于滚动/点击
        draggedAppointment?.let {
            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    // 1. 更新手指的当前位置
                    dragCurrentX = event.x
                    dragCurrentY = event.y

                    // 2. 更新占位符位置
                    updatePlaceholder(event.x, event.y)

                    postInvalidateOnAnimation()
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 🌟 核心：处理拖拽放下/取消
                    handleDrop()
                    // 重置拖拽状态
                    draggedAppointment = null
                    isDragging = false
                    placeholderCell = null
                    placeholderIndex = null
                    originalDayOffset = -1
                    originalPeriod = -1

                    // 清除惯性
                    if (!scroller.isFinished) {
                        scroller.abortAnimation()
                    }
                    invalidate()
                    return true
                }

                else -> {}
            }
        }

        // 处理滚动、点击和长按
        gestureDetector.onTouchEvent(event)
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var lastScaleTime = 0L

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastScaleTime < 10) return true
            onScaling = true

            // 缩放中停止惯性
            if (!scroller.isFinished) {
                scroller.abortAnimation()
            }

            val oldColWidth = colWidth
            var factor = detector.scaleFactor
            factor = 1 + (factor - 1) * 0.5f // 减缓缩放速度

            // 反比例 factor越大 可显示日期越少
            daysShownFloat =
                (daysShownFloat / factor).coerceIn(minDays.toFloat(), maxDays.toFloat())
            updateColWidth() // 更新 colWidth

            // 计算缩放导致的 offsetX 变化（保持焦点不变）
            val focusX = detector.focusX
            // 滚动区域的相对X坐标
            val relFocusX = focusX - leftColumnWidth
            // (offsetX + relFocusX) 是焦点的真正位置  (colWidth / oldColWidth) 缩放比例
            offsetX =
                ((offsetX + relFocusX) * (colWidth / oldColWidth) - relFocusX)

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
        if (!onScaling && !isDragging) {
            val maxX = reCalculateMaxX()
            // 更新 offsetX，并约束其范围
            offsetX = (offsetX + distanceX).coerceIn(0f, maxX)
            postInvalidateOnAnimation()
        }
        return true
    }

    override fun onLongPress(e: MotionEvent) {
        if (onScaling) return

        // 找到长按的item
        val result = findAppointmentAt(e.x, e.y)
        draggedAppointment = result?.first

        draggedAppointment?.let { appt ->
            isDragging = true
            // 🌟 新增：记录原始位置
            originalDayOffset = appt.dayOffset
            originalPeriod = appt.period

            // 🌟 优化3: 记录 Item 左上角相对于手指点击点的偏移量
            dragOffsetX = result!!.second // Item Left - Click X
            dragOffsetY = result.third // Item Top - Click Y

            // 设置当前拖拽位置
            dragCurrentX = e.x
            dragCurrentY = e.y

            // 立即更新占位符位置
            updatePlaceholder(e.x, e.y)

            invalidate()
        }
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (isDragging) return false // 拖拽时不处理惯性

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

    /**
     * 根据当前手指位置 (x, y) 实时计算并更新占位符的位置 (placeholderCell, placeholderIndex)
     */
    private fun updatePlaceholder(x: Float, y: Float) {
        // 如果手指在左侧固定列或标题栏，则不处理占位符（不移动占位符）
        if (x < leftColumnWidth || y < headerHeight) return

        val leftStart = leftColumnWidth - offsetX

        // 1. 计算新的单元格
        val relX = x - leftStart
        val newDay = floor(relX / colWidth).toInt().coerceIn(0, maxDays - 1)
        val newPeriod = floor((y - headerHeight) / rowHeight).toInt().coerceIn(0, 1)

        placeholderCell = newDay to newPeriod

        // 2. 计算新 Cell 内的插入索引
        val baseTop = headerHeight + newPeriod * rowHeight
        val itemH = rowHeight / maxPerCell

        // 计算 Cell 内的相对 Y 坐标
        val relYInCell = y - baseTop

        // 向上取整计算索引（floor(relYInCell / itemH) 得到的是经过了多少个完整 item 高度）
        val newIndex = floor(relYInCell / itemH).toInt()

        // 约束索引：不能超过该 Cell 中已有日程数量（除了被拖动的那一个）
        val cellAppointmentsCount = appointments.count {
            it.dayOffset == newDay && it.period == newPeriod && it != draggedAppointment
        }

        placeholderIndex = newIndex.coerceIn(0, cellAppointmentsCount)
    }

    /**
     * 放置日程。根据占位符的位置更新数据列表，并处理拦截回调。
     */
    private fun handleDrop() {
        val appt = draggedAppointment ?: return
        val targetCell = placeholderCell
        val targetIndex = placeholderIndex

        // 目标位置无效、或者目标位置与原位置相同，则日程回到原位，不触发回调
        if (targetCell == null || targetIndex == null ||
            (targetCell.first == originalDayOffset && targetCell.second == originalPeriod)
        ) {
            // Log.d("Drop", "Invalid or Same location. Revert.")
            // 此时 appt 已经在列表中，无需处理，因为 onLongPress 并没有删除它
            // 只需要确保它的 dayOffset 和 period 保持不变即可（它们本来就没变）
            return
        }

        val (newDay, newPeriod) = targetCell
        val oldDay = originalDayOffset
        val oldPeriod = originalPeriod

        // 🌟 拦截逻辑：在数据变更前调用回调
        val proceed = onBeforeAppointmentDropListener?.invoke(
            appt,
            newDay,
            newPeriod,
            targetIndex
        ) ?: true // 如果未设置拦截器，则默认为 true (继续)

        if (proceed) {
            // 确认修改：进行数据变更
            // 1. 从appointments集合中删除被拖动的日程
            appointments.remove(appt)

            // 2. 插入到新的位置
            val originalList = appointments.toMutableList()
            appointments.clear()

            var currentListIndex = 0
            originalList.forEach { item ->
                if (item.dayOffset == newDay && item.period == newPeriod) {
                    // 目标 Cell
                    if (currentListIndex == targetIndex) {
                        appointments.add(appt) // 插入被拖动的 Item
                    }
                    appointments.add(item) // 插入原有的 Item
                    currentListIndex++
                } else {
                    // 非目标 Cell
                    appointments.add(item)
                }
            }

            // 处理目标索引在列表末尾的情况
            if (currentListIndex == targetIndex) {
                appointments.add(appt)
            }

            // 3. 更新被拖动日程的 Cell 信息
            appt.dayOffset = newDay
            appt.period = newPeriod

            // 🌟 触发移动成功回调
            onAppointmentMovedListener?.invoke(appt, oldDay, oldPeriod)
//            Toast.makeText(this.context, appt.name + "移动到" + newDay + newPeriod, Toast.LENGTH_SHORT).show()
        } else {
            // 取消修改：不做数据变更，日程回到原位
            // 由于 onLongPress 时没有从列表中移除，所以只需忽略即可。
            // 实际上，如果拖拽到不同的位置，但 onBeforeAppointmentDropListener 返回 false，
            // 那么 appt 的 dayOffset 和 period 依然是原值，它会自然地被绘制回原位。
        }
    }

    /**
     * 找到给定坐标下的日程
     * @return Triple<Appointment, Float, Float> - 日程对象, X偏移量(Item左上角-点击X), Y偏移量(Item左上角-点击Y)
     */
    private fun findAppointmentAt(x: Float, y: Float): Triple<Appointment, Float, Float>? {
        val leftStart = leftColumnWidth - offsetX
        val dayIndex = floor((x - leftStart) / colWidth).toInt()
        val period = floor((y - headerHeight) / rowHeight).toInt()

        // 过滤出该 Cell 中的所有日程
        val cellAppointments =
            appointments.filter { it.dayOffset == dayIndex && it.period == period }

        val baseTop = headerHeight + period * rowHeight
        val itemH = rowHeight / maxPerCell
        val itemDrawH = itemH - itemPadding * 2 // 实际绘制高度

        cellAppointments.forEachIndexed { index, appointment ->
            val top = baseTop + index * itemH + itemPadding // Item 左上角 Y
            val left = leftColumnWidth - offsetX + dayIndex * colWidth + itemPadding // Item 左上角 X
            // 调整 RectF 的计算，确保它代表整个可见区域的矩形
            val hitRect = RectF(
                max(left, leftColumnWidth + itemPadding), // 考虑左侧裁剪
                top,
                left + colWidth - itemPadding,
                top + itemDrawH
            )
            if (hitRect.contains(x, y)) {
                // 计算 Item 左上角到点击点的偏移量
                val dx = hitRect.left - x
                val dy = hitRect.top - y
                return Triple(appointment, dx, dy)
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