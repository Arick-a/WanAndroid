package com.example.module_usercenter.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.module_usercenter.R


class CommentAdapter(private val items: List<String>) : RecyclerView.Adapter<CommentAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return VH(v)
    }


    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(items[position])
        }
    }

    var onItemClickListener: ((String) -> Unit)? = null

    override fun getItemCount(): Int = items.size
}