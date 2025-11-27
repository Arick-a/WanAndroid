package com.example.module_usercenter.adapter

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.module_usercenter.R
import com.example.module_usercenter.bean.StaggeredItem
import com.example.module_usercenter.bean.preMeasureItems

class StaggeredAdapter :
    ListAdapter<StaggeredItem, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<StaggeredItem>() {
            override fun areItemsTheSame(old: StaggeredItem, new: StaggeredItem) =
                old.id == new.id

            override fun areContentsTheSame(old: StaggeredItem, new: StaggeredItem) =
                old == new
        }
    }

    // === 修改：添加可选的完成回调 ===
    fun submitListWithPreMeasure(list: List<StaggeredItem>, context: Context, commitCallback: (() -> Unit)? = null) {
        // 计算 item 宽度
        val displayWidth = context.resources.displayMetrics.widthPixels
        val itemWidth = (displayWidth - dp2px(context, 30)) / 2 // padding + gap

        preMeasureItems(context, list, itemWidth)

        // 使用 commitCallback
        submitList(list, commitCallback)
    }

    fun submitListWithPreMeasure(list: List<StaggeredItem>, context: Context) {
        // 计算 item 宽度
        val displayWidth = context.resources.displayMetrics.widthPixels
        val itemWidth = (displayWidth - dp2px(context, 30)) / 2 // padding + gap

        preMeasureItems(context, list, itemWidth)

        submitList(list)
    }

    override fun getItemViewType(position: Int) = getItem(position).type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            StaggeredItem.TYPE_TEXT ->
                TextHolder(inflater.inflate(R.layout.item_text, parent, false))

            else ->
                ImageHolder(inflater.inflate(R.layout.item_image, parent, false))
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        // 设置高度
        holder.itemView.layoutParams.height = item.measuredHeight

        if (holder is TextHolder) {
            holder.title.text = item.title
            holder.content.text = item.content

            // === 居中展示逻辑 ===
            // 当实际内容高度（标题+内容）小于 item.measuredHeight 时，将内容居中
            if (item.measuredHeight > 0) {
                // 设置容器的重力为垂直居中
                holder.container.foregroundGravity = android.view.Gravity.CENTER_VERTICAL
            } else {
                // 否则靠上对齐 (默认)
                holder.container.foregroundGravity = android.view.Gravity.TOP
            }
        }

        holder.itemView.layoutParams.height = item.measuredHeight
        if (holder is ImageHolder) {
            holder.image.setImageResource(item.imageRes)
        }
    }

    class TextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 引用 item_text.xml 的根布局
        val container: ViewGroup = itemView.findViewById(R.id.llContainer) as ViewGroup
        val title: TextView = itemView.findViewById(R.id.tvTitle) // 绑定标题
        val content: TextView = itemView.findViewById(R.id.tvContent) // 绑定内容
    }

    class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.ivPic)
    }
}

fun dp2px(context: Context, dp: Int): Int {
    val scale = context.resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}
