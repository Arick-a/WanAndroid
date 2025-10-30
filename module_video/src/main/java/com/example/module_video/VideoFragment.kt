package com.example.module_video

import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.common_base.base.mvvm.BaseMvvmFragment
import com.example.common_base.base.viewmodel.BaseViewModel
import com.example.common_base.constants.AConstance
import com.example.module_video.databinding.FragmentVideoBinding
import com.example.module_video.recommend.RecommendFragment
import com.google.android.material.tabs.TabLayoutMediator

/**
 *  @author : zhang.wenqiang
 *  @date : 2020/12/14
 *  description :
 */
@Route(path = AConstance.FRAGMENT_URL_VIDEO)
class VideoFragment : BaseMvvmFragment<FragmentVideoBinding, BaseViewModel>() {
    private val fragments: MutableList<Fragment> = mutableListOf()
    private val titles: MutableList<String> = mutableListOf("推荐")

    init {
        ARouter.getInstance().inject(this)
    }

    override fun initView(view: View?) {

    }

    override fun initData() {
        super.initData()
        fragments.add(RecommendFragment())

        viewDataBinding.pager.adapter = object :
            FragmentStateAdapter(requireActivity().supportFragmentManager, lifecycle) {
            override fun getItemCount(): Int = fragments.size

            override fun createFragment(position: Int) = fragments[position]
        }

        TabLayoutMediator(viewDataBinding.tabLayout, viewDataBinding.pager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }

    override fun getLayoutResId(): Int =
        R.layout.fragment_video

    override fun createViewModel(): BaseViewModel {
        return BaseViewModel()
    }
}