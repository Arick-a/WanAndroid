package com.example.module_usercenter.bean

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.module_usercenter.adapter.dp2px

data class StaggeredItem(
    val id: Int,
    val type: Int,
    val title: String? = null, // 新增：用于标题
    val content: String? = null, // 新增：用于内容
    val text: String? = null,
    val imageRes: Int = 0,
) {
    var measuredHeight: Int = 0

    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_IMAGE = 2
    }
}

fun preMeasureItems(context: Context, list: List<StaggeredItem>, itemWidth: Int) {
    // 标题 TextView 配置
    val tmpTitleView = TextView(context).apply {
        textSize = 16f // 对应 item_text.xml 中的 16sp
        maxLines = 6   // 对应 item_text.xml 中的 maxLines
        layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // 内容 TextView 配置
    val tmpContentView = TextView(context).apply {
        textSize = 14f // 对应 item_text.xml 中的 14sp
        maxLines = Int.MAX_VALUE // 内容不限制行数
        layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    list.forEach { item ->
        if (item.type == StaggeredItem.TYPE_TEXT) {

            // 1. 测量标题高度
            tmpTitleView.text = item.title
            var totalHeight = measureTextHeight(tmpTitleView, itemWidth)

            // 2. 测量内容高度
            tmpContentView.text = item.content
            totalHeight += measureTextHeight(tmpContentView, itemWidth)

            // 3. 加上 padding 和 margin
            val padding = dp2px(context, 10) // item_text.xml 中的总 padding
            val marginBottom = dp2px(context, 4) // 标题和内容之间的 margin
            totalHeight += (2 * padding) + marginBottom

            // 高度限制：3:4 最小，4:3 最大
            val minH = (itemWidth * 3) / 4
            val maxH = (itemWidth * 4) / 3 // 假设最大高度是宽度的 4/3 倍

            item.measuredHeight = totalHeight.coerceIn(minH, maxH)

        } else {
            // 图片默认 4:3
            item.measuredHeight = (itemWidth * 4) / 3
        }
    }
}

// 提取测量逻辑为单独函数
fun measureTextHeight(textView: TextView, itemWidth: Int): Int {
    val wSpec = View.MeasureSpec.makeMeasureSpec(itemWidth, View.MeasureSpec.EXACTLY)
    val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    textView.measure(wSpec, hSpec)
    return textView.measuredHeight
}