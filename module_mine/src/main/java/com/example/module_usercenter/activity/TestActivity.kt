package com.example.module_usercenter.activity

import android.annotation.SuppressLint
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
import com.example.module_usercenter.widget.HandleControlledBottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.youth.banner.util.BannerUtils.dp2px
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

@Route(path = AConstance.ACTIVITY_URL_SPLASH)
class TestActivity : BaseActivity() {
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var recyclerView: RecyclerView

    override fun getLayoutResId(): Int = R.layout.activity_test

    override fun initView() {
        StatusBarUtil.setDarkMode(this, true)
        testFun()
        cooView()
    }

    private lateinit var btnConfirm: Button

    @SuppressLint("ClickableViewAccessibility")
    private fun cooView() {
        val bottomSheet = findViewById<LinearLayout>(R.id.bottom_sheet)
        recyclerView = findViewById(R.id.recyclerView)
        btnConfirm = findViewById(R.id.btn_confirm)

        // 初始化BottomSheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet).apply {
            peekHeight = 500 // 默认展示两条
            isHideable = false
        }

        // 监听 BottomSheet 拖动状态
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // slideOffset: -1(隐藏) ~ 0(收起) ~ 1(展开)
                val translationY = slideOffset.coerceIn(0f, 1f)
                btnConfirm.translationY = translationY * btnConfirm.height
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = CommentAdapter(generateMock())
        recyclerView.isNestedScrollingEnabled = true
    }

    private fun generateMock(): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..30) list.add("Comment item #$i")
        return list
    }

    override fun initData() {

    }

    private fun testFun() {
        findViewById<Button>(R.id.btn_1).setOnClickListener {
            funA()
        }
        findViewById<Button>(R.id.btn_2).setOnClickListener {
            haha(10000000)
        }
        findViewById<Button>(R.id.btn_3).setOnClickListener {
            funD(1000)
        }
    }

    private fun haha(l: Long): Double {
        val haha2 = haha2(l)
        val haha3 = haha3(l)
        return haha2 + haha3
    }

    private fun haha2(l: Long): Double {
        var result = 0.0
        for (i in 0 until l) {
            result += acos(cos(i.toDouble()))
            result -= asin(sin(i.toDouble()))
        }
        return result
    }

    private fun haha3(l: Long): Double {
        var result = 0.0
        for (i in 0 until l) {
            result += acos(cos(i.toDouble()))
            result -= asin(sin(i.toDouble()))
        }
        return result
    }

    private fun funD(i: Int) {
        if (i == 1) {
            Log.e("99788", "1*1=1")
        } else {
            for (j in 1 until i) {
                Log.e("99788", j.toString() + "*" + i + "=" + j * i + " ")
            }
            funD(i - 1)
        }
    }

    fun funA() {
        Thread.sleep(110)
        funB()
    }

    fun funB() {
        Thread.sleep(812)
        funC()
    }

    fun funC() {
        Thread.sleep(101)
    }

}
