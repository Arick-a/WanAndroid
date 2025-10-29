package com.example.module_main.ui

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.common_base.base.BaseActivity
import com.example.common_base.constants.AConstance
import com.example.common_base.constants.FlutterConstance.FROM_FLUTTER_EVENT_COOKIE
import com.example.common_base.constants.FlutterConstance.TO_FLUTTER_EVENT_COOKIE
import com.example.common_base.util.CookieHelper
import com.example.common_base.util.GlobalThreadPools
import com.example.common_base.util.StatusBarUtil
import com.example.module_main.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
//import com.idlefish.flutterboost.EventListener
//import com.idlefish.flutterboost.FlutterBoost
//import com.idlefish.flutterboost.ListenerRemover
import java.util.*

@Route(path = AConstance.ACTIVITY_URL_MAIN)
class MainActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private var tabList = mutableListOf<TabItemBean>()
//    private lateinit var remover: ListenerRemover;
    override fun getLayoutResId(): Int = R.layout.activity_main

    override fun initView() {
        StatusBarUtil.setTranslucentForImageViewInFragment(this, 0, null)
        StatusBarUtil.setLightMode(this)

        createTabData()

        initViewPager()

        initFlutterEvent()
    }

    private fun initFlutterEvent() {
//        val listener = EventListener { _, _ ->
//            FlutterBoost.instance()
//                .sendEventToFlutter(TO_FLUTTER_EVENT_COOKIE, CookieHelper.getDefCookieMap())
//        }
//        remover = FlutterBoost.instance().addEventListener(FROM_FLUTTER_EVENT_COOKIE, listener)
    }

    private fun initViewPager() {
        viewPager = findViewById(R.id.fl_main_container)
        viewPager.isUserInputEnabled = false
        val screenSlidePagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = screenSlidePagerAdapter
        val tabLayout = findViewById<TabLayout>(R.id.main_tablayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabList[position].tabTitle
        }.attach()

        for (i in 0 until screenSlidePagerAdapter.itemCount) {
            tabLayout.getTabAt(i)?.run {
                setCustomView(R.layout.main_tab_item)
                customView!!.findViewById<ImageView>(R.id.main_iv_icon)
                    .setBackgroundResource(if (i == 0) tabList[i].tabSelectedIcon else tabList[i].tabUnSelectedIcon)
                (customView!!.findViewById<View>(R.id.main_tv_title) as TextView).text =
                    tabList[i].tabTitle
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(p0: TabLayout.Tab?) {

            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
                p0!!.customView!!.findViewById<ImageView>(R.id.main_iv_icon).setBackgroundResource(
                    tabList[p0.position].tabUnSelectedIcon
                )
            }

            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0!!.customView!!.findViewById<ImageView>(R.id.main_iv_icon)
                    .setBackgroundResource(tabList[p0.position].tabSelectedIcon)

                when (p0.position) {
                    0, 1 -> StatusBarUtil.setLightMode(this@MainActivity)
                    2 -> StatusBarUtil.setDarkMode(this@MainActivity)
                    else -> {
                    }
                }
            }
        })
    }

    private fun createTabData() {
        tabList.apply {
            add(TabItemBean("文章", R.drawable.icon_menu_home_sel, R.drawable.icon_menu_home_unsel))
            add(
                TabItemBean(
                    "视频",
                    R.drawable.icon_menu_project_sel,
                    R.drawable.icon_menu_project_unsel
                )
            )
            add(TabItemBean("我的", R.drawable.icon_menu_mine_sel, R.drawable.icon_menu_mine_unsel))
        }
    }

    override fun initData() {

    }

    override fun onDestroy() {
        super.onDestroy()
//        remover.remove()
    }

    private inner class TabItemBean(
        var tabTitle: String,
        var tabSelectedIcon: Int,
        var tabUnSelectedIcon: Int
    )

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        private var fragments = mutableListOf<Fragment>()

        init {
            fragments.apply {
                add(
                    ARouter.getInstance().build(AConstance.FRAGMENT_URL_HOME)
                        .navigation() as Fragment
                )
                add(
                    ARouter.getInstance().build(AConstance.FRAGMENT_URL_VIDEO)
                        .navigation() as Fragment
                )
                add(
                    ARouter.getInstance().build(AConstance.FRAGMENT_URL_MINE)
                        .navigation() as Fragment
                )
            }
        }

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}
