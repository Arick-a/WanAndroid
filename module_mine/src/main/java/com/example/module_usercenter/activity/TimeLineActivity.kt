package com.example.module_usercenter.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common_base.base.BaseActivity
import com.example.common_base.constants.AConstance
import com.example.common_base.util.StatusBarUtil
import com.example.module_usercenter.R
import com.example.module_usercenter.adapter.CommentAdapter
import com.example.module_usercenter.adapter.TimelineAdapter
import com.example.module_usercenter.bean.TimelineNode
import com.example.module_usercenter.widget.HandleControlledBottomSheetBehavior
import com.example.module_usercenter.widget.TimelineItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.youth.banner.util.BannerUtils.dp2px
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

@Route(path = "")
class TimeLineActivity : BaseActivity() {
    private lateinit var rv : RecyclerView

    override fun getLayoutResId(): Int = R.layout.activity_time_line

    override fun initView() {
        StatusBarUtil.setDarkMode(this, true)
        testFun()
        cooView()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun cooView() {
        rv  = findViewById(R.id.recyclerView)
        val nodes = (1..15).map { index ->
            when {
                index < 10 -> TimelineNode("节点 $index", "2025-11-${(index % 30) + 1}", isDone = true)
                index == 10 -> TimelineNode("节点 $index", "当前节点", isCurrent = true)
                else -> TimelineNode("节点 $index", null)
            }
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = TimelineAdapter(nodes)
        rv.addItemDecoration(TimelineItemDecoration(nodes))
    }

    override fun initData() {

    }

    private fun testFun() {

    }
}
