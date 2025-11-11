package com.example.module_usercenter.widget

import com.example.module_usercenter.bean.TimelineNode

import android.graphics.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.toColorInt
import com.example.module_usercenter.R

class TimelineItemDecoration(
    private val data: List<TimelineNode>,
    private val lineColor: Int = Color.LTGRAY,
    private val doneColor: Int = "#2196F3".toColorInt(),
    private val currentColor: Int = "#4CAF50".toColorInt()
) : RecyclerView.ItemDecoration() {

    private val circleRadius = 10f
    private val lineWidth = 4f
    private val centerX = 50f  // 轴心横坐标（对应 item 的 paddingStart）
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        strokeWidth = lineWidth
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        val itemCount = data.size

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            if (position == RecyclerView.NO_POSITION) continue

            val node = data[position]

            val titleView = child.findViewById<TextView>(R.id.tvTitle)
            val centerY = child.top + (titleView.top + titleView.bottom) / 2f

            // 画连线（上）
            if (position > 0) {
                paint.color = if (node.isDone || node.isCurrent) doneColor else lineColor
                c.drawLine(centerX, child.top.toFloat(), centerX, centerY - circleRadius, paint)
            }

            // 画连线（下）
            if (position < itemCount - 1) {
                val nextNode = data[position + 1]
                paint.color = if (node.isDone || node.isCurrent) doneColor else lineColor
                c.drawLine(centerX, centerY + circleRadius, centerX, child.bottom.toFloat(), paint)
            }

            // 画节点
            paint.color = when {
                node.isCurrent -> currentColor
                node.isDone -> doneColor
                else -> lineColor
            }
            c.drawCircle(centerX, centerY, circleRadius, paint)
        }
    }
}
