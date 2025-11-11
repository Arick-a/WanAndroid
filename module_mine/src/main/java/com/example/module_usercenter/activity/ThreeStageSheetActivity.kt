package com.example.module_usercenter.activity

import android.annotation.SuppressLint
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common_base.base.BaseActivity
import com.example.common_base.util.StatusBarUtil
import com.example.module_usercenter.R
import com.example.module_usercenter.adapter.CommentAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior


@Route(path = "")
class ThreeStageSheetActivity : BaseActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun getLayoutResId(): Int = R.layout.activity_three_stage

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        StatusBarUtil.setDarkMode(this, true)

        val bottomSheet = findViewById<LinearLayout>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        val screenHeight = resources.displayMetrics.heightPixels
        val expandedOffset = (screenHeight * 0.15).toInt()

        bottomSheetBehavior.isFitToContents = false
        bottomSheetBehavior.peekHeight = (screenHeight * 0.2).toInt()
        bottomSheetBehavior.halfExpandedRatio = 0.5f
        bottomSheetBehavior.expandedOffset = expandedOffset
        bottomSheetBehavior.isHideable = false

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        rv.setPadding(0, 0, 0, expandedOffset)
        rv.clipToPadding = false
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = CommentAdapter(generateMock())

        // ✅ 关键：使用 OnScrollListener + canScrollVertically 判断
        rv.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val canScrollUp = recyclerView.canScrollVertically(-1)
                val canScrollDown = recyclerView.canScrollVertically(1)
                val state = bottomSheetBehavior.state

                // 折叠态：RV 可以滚动时，禁止 BottomSheet 拦截
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (canScrollDown) {
                        recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                    } else {
                        recyclerView.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }

                // 半展开 / 展开态：交给 RV 处理
                if (state == BottomSheetBehavior.STATE_HALF_EXPANDED || state == BottomSheetBehavior.STATE_EXPANDED) {
                    recyclerView.parent.requestDisallowInterceptTouchEvent(true)
                }
            }
        })
    }




    private fun generateMock(): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..30) list.add("Comment item #$i")
        return list
    }

    override fun initData() {
    }
}