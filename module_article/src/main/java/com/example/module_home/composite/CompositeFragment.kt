package com.example.module_home.composite

import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.common_base.base.mvvm.BaseMvvmFragment
import com.example.module_home.ArticleViewModelFactory
import com.example.module_home.R
import com.example.module_home.databinding.FragmentCompositeBinding
import com.example.module_home.home.ArticleViewModel
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 综合
 */
class CompositeFragment : BaseMvvmFragment<FragmentCompositeBinding, ArticleViewModel>() {

    override fun initData() {
        super.initData()
        viewModel.getTree()
    }

    override fun addObserver() {
        super.addObserver()
        viewModel.treeData.observe(this, {

            viewDataBinding.pager.offscreenPageLimit = it.size

            viewDataBinding.pager.adapter = object :
                FragmentStateAdapter(requireActivity().supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = it.size

                override fun createFragment(position: Int) =
                    CompositePageFragment.newInstance(it[position].id)
            }

            TabLayoutMediator(viewDataBinding.tabLayout, viewDataBinding.pager) { tab, position ->
                tab.text = it[position].name
            }.attach()

        })
    }

    override fun createViewModel(): ArticleViewModel {
        return ArticleViewModelFactory().create(ArticleViewModel::class.java)
    }

    override fun getLayoutResId(): Int = R.layout.fragment_composite
}