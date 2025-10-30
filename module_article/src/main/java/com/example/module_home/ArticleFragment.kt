package com.example.module_home

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Debug
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common_base.base.mvvm.BaseMvvmFragment
import com.example.common_base.constants.AConstance
import com.example.common_base.performance.TIME_MONITOR_APP_ONCREATE
import com.example.common_base.performance.TimeMonitorManager
import com.example.common_base.web.WebViewActivity
import com.example.module_home.composite.CompositeFragment
import com.example.module_home.databinding.ArticleFragmentHomeBinding
import com.example.module_home.home.ArticleViewModel
import com.example.module_home.home.HomeFragment
import com.example.module_home.home.adapter.HomeBannerAdapter
import com.example.module_home.home.bean.BannerBean
import com.example.module_home.search.SearchActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.youth.banner.indicator.RectangleIndicator

/**
 * Home页
 */
@Route(path = AConstance.FRAGMENT_URL_HOME)
class ArticleFragment : BaseMvvmFragment<ArticleFragmentHomeBinding, ArticleViewModel>() {

    private val fragments: MutableList<Fragment> = mutableListOf()
    private val titles: MutableList<String> = mutableListOf("首页", "综合")

    @RequiresApi(Build.VERSION_CODES.M)
    override fun initView(view: View?) {
        fragments.add(HomeFragment())
        fragments.add(CompositeFragment())

        // GC使用的总耗时，单位是毫秒
        val runtimeStat = Debug.getRuntimeStat("art.gc.gc-time")
        // 阻塞式GC的总耗时
        val runtimeStat1 = Debug.getRuntimeStat("art.gc.blocking-gc-time")

        Log.d("99788", "GC使用的总耗时:$runtimeStat /阻塞式GC的总耗时:$runtimeStat1")

        viewDataBinding.pager.adapter = object :
            FragmentStateAdapter(requireActivity().supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = fragments.size

            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(viewDataBinding.tabLayout, viewDataBinding.pager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        viewDataBinding.searchInput.apply {
            queryHint = getString(R.string.str_input_key)
            setOnClickListener {
                startActivity(
                    Intent(
                        requireActivity(),
                        SearchActivity::class.java
                    )
                )
            }
        }

        viewDataBinding.banner.apply {
            val screenWidth: Int = Resources.getSystem().displayMetrics.widthPixels
            val screenHeight: Int = Resources.getSystem().displayMetrics.heightPixels
            val height = (screenWidth.coerceAtMost(screenHeight) * (9f / 16f)).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            )
            addBannerLifecycleObserver(this@ArticleFragment)
            adapter = HomeBannerAdapter(requireContext(), mutableListOf())
            indicator = RectangleIndicator(requireContext())
            setBannerGalleryMZ(10)
            setOnBannerListener { data, _ ->
                WebViewActivity.launch(requireActivity(), (data as BannerBean).url)
            }
        }
        viewModel.getBanner()
        TimeMonitorManager.getTimeMonitor(TIME_MONITOR_APP_ONCREATE)
            .recordingTimeTag("ArticleFragment-initView-end")
    }

    override fun addObserver() {
        super.addObserver()
        viewModel.bannersData.observe(viewLifecycleOwner) {
            viewDataBinding.banner.setDatas(it)
        }
    }

    override fun getLayoutResId(): Int =
        R.layout.article_fragment_home

    override fun createViewModel(): ArticleViewModel {
        return ArticleViewModelFactory().create(ArticleViewModel::class.java)
    }
}


