package com.example.module_usercenter.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.module_usercenter.R
import kotlin.math.abs

/**
 * 一个强化版 SmartBottomSheetLayout：
 * ✅ 支持折叠 / 半展开 / 全展开
 * ✅ 支持滑动进度监听 (onSlideListener)
 * ✅ 支持 RecyclerView 内部滚动
 * ✅ 默认露出一部分内容，避免按钮遮挡
 */
class SmartBottomSheetLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var dragHelper: ViewDragHelper
    private var contentView: View? = null

    private var minTop = 0      // 折叠位置 top（底部）
    private var midTop = 0      // 半展开位置 top
    private var maxTop = 0      // 全展开 top（顶部）

    private var touchSlop = 0
    private var initialY = 0f
    private var isDragging = false

    private var stateChangeListener: ((Int) -> Unit)? = null
    private var onSlideListener: ((Float) -> Unit)? = null  // 👈 新增

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        dragHelper = ViewDragHelper.create(this, 1.0f, DragCallback())
        dragHelper.setMinVelocity(1000f)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) contentView = getChildAt(0)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (contentView == null) return

        val parentH = height
        // 确保 top 值非负并基于视口计算
        maxTop = paddingTop.coerceAtLeast(0)
        midTop = parentH / 2
        val peekHeight = dp(220)
        minTop = (parentH - peekHeight).coerceAtLeast(maxTop)

        // 立即将 content 放到折叠位置（如果尚未放置）
        if (contentView!!.top != minTop) {
            contentView!!.offsetTopAndBottom(minTop - contentView!!.top)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // always feed dragHelper for internal tracking
        try { dragHelper.shouldInterceptTouchEvent(ev) } catch (e: Exception) {}

        val rv = contentView?.findViewById<RecyclerView>(R.id.sheetRecycler)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = ev.y
                isDragging = false
                // let child decide first
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = ev.y - initialY
                // downward intention
                if (dy > touchSlop) {
                    if (rv == null) {
                        return true
                    }
                    // 如果 rv 已经在顶部，直接拦截
                    if (!rv.canScrollVertically(-1)) {
                        // 停止 RV 的 fling/scroll（避免惯性干扰）
                        rv.stopScroll()
                        // 给 RV 发一个 cancel，让它放弃事件流（这样 parent 有机会拦截）
                        val cancel = MotionEvent.obtain(ev)
                        cancel.action = MotionEvent.ACTION_CANCEL
                        rv.dispatchTouchEvent(cancel)
                        cancel.recycle()
                        return true
                    } else {
                        // RV 还没到顶，**但**它可能会在接下来几帧滚动到顶（用户向下拖）
                        // 我们不立刻抢占，但可以做：如果 RV 的当前滚动偏移接近顶（<= touchSlop），也抢占
                        try {
                            val offset = rv.computeVerticalScrollOffset()
                            if (offset <= touchSlop) {
                                rv.stopScroll()
                                val cancel = MotionEvent.obtain(ev)
                                cancel.action = MotionEvent.ACTION_CANCEL
                                rv.dispatchTouchEvent(cancel)
                                cancel.recycle()
                                return true
                            }
                        } catch (t: Throwable) { /* ignore on exotic RV */ }
                        return false
                    }
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return false
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = contentView?.findViewById<RecyclerView>(R.id.sheetRecycler)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialY = event.y
                // 如果 RV fling，立即停止（用户显式按下意味着想接管）
                rv?.stopScroll()
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - initialY
                if (!isDragging && abs(dy) > touchSlop) {
                    isDragging = true
                }

                // 如果正在拖动且 rv 在顶（或不存在），阻止父层拦截（保持我们处理）
                if (isDragging && dy > 0 && (rv == null || !rv.canScrollVertically(-1))) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }

        // 让 ViewDragHelper 处理位移/settle
        try { dragHelper.processTouchEvent(event) } catch (e: Exception) {}
        return true
    }

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            val top = contentView?.top ?: 0
            val progress = 1f - (top - maxTop).toFloat() / (minTop - maxTop)
            onSlideListener?.invoke(progress.coerceIn(0f,1f))
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            // settle 完成后再校正一次（防止轻微越界）
            contentView?.let {
                val t = it.top.coerceIn(maxTop, minTop)
                if (it.top != t) {
                    it.offsetTopAndBottom(t - it.top)
                }
            }
        }
    }

    private inner class DragCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (child !== contentView) return false

            val rv = child.findViewById<RecyclerView>(R.id.sheetRecycler)
            if (rv != null && rv.canScrollVertically(-1)) {
                return false
            }

            rv?.stopScroll()
            return true
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val clamped = top.coerceIn(maxTop, minTop)
            val progress = 1f - (clamped - maxTop).toFloat() / (minTop - maxTop)
            onSlideListener?.invoke(progress.coerceIn(0f, 1f))
            return clamped
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            val settleTop = when {
                yvel > 1500 -> minTop  // 向下快速滑动 → 折叠
                yvel < -1500 -> maxTop  // 向上快速滑动 → 展开
                else -> {
                    // 根据当前位置选择最近的状态
                    val cur = releasedChild.top
                    val dMax = abs(cur - maxTop)
                    val dMid = abs(cur - midTop)
                    val dMin = abs(cur - minTop)
                    when (minOf(dMax, dMid, dMin)) {
                        dMax -> maxTop
                        dMid -> midTop
                        else -> minTop
                    }
                }
            }

            dragHelper.settleCapturedViewAt(releasedChild.left, settleTop)
            invalidate()

            val progress = 1f - (settleTop - maxTop).toFloat() / (minTop - maxTop)
            onSlideListener?.invoke(progress)

            val newState = when (settleTop) {
                minTop -> 0   // 折叠
                midTop -> 1   // 半展开
                else -> 2     // 展开
            }
            stateChangeListener?.invoke(newState)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
