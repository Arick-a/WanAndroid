package com.example.module_usercenter.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * 修正版 BottomSheetBehavior
 * - 当内部 RecyclerView 还能滚动时，不让 BottomSheet 预先消费滑动事件（让 RecyclerView 自己滚动）
 * - 只有当 RecyclerView 滚动不到时，才交给父类处理（BottomSheet 展开/收起）
 */
class HandleControlledBottomSheetBehavior<V : View>(
    context: Context,
    attrs: AttributeSet?
) : BottomSheetBehavior<V>(context, attrs) {

    private var nestedRecyclerView: RecyclerView? = null

    // 先捕获 nested child（可能为 RecyclerView）
    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int
    ) {
        super.onStopNestedScroll(coordinatorLayout, child, target, type)
        // 可以在这里清理引用，避免长期持有（若你愿意也可以保留）
        // nestedRecyclerView = null
    }

    override fun onNestedPreScroll(
        parent: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        // 如果 target 是 RecyclerView，则判断是否能继续向上/下滚动
        if (target is RecyclerView) {
            val canScrollUp = target.canScrollVertically(-1)
            val canScrollDown = target.canScrollVertically(1)

            // 🔒 case 1：RV 可以滚动，就不让 bottomsheet 消费事件
            if ((dy > 0 && canScrollDown) || (dy < 0 && canScrollUp)) {
                // RV自己消费滑动，不交给bottomsheet
                return
            }
        }

        // 其他情况（RV滚不动了），才交给父类处理
        super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type)
    }

    // 可选：强化 fling 行为，如果你遇到 fling 导致父子竞争的问题可以在这里做处理
    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (target is RecyclerView) {
            // 如果 RecyclerView 能继续在 fling 方向滚动，则不要拦截 fling
            if (velocityY < 0 && target.canScrollVertically(-1)) {
                return false
            }
            if (velocityY > 0 && target.canScrollVertically(1)) {
                return false
            }
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
    }
}