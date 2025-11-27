package com.example.module_usercenter.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpacingDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = space
        outRect.right = space
        outRect.top = space
        outRect.bottom = space
    }
}