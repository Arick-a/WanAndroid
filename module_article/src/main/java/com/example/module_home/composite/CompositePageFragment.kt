package com.example.module_home.composite

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.common_base.base.mvvm.BaseMvvmFragment
import com.example.common_base.base.viewmodel.CompleteState
import com.example.common_base.base.viewmodel.ErrorState
import com.example.common_base.base.viewmodel.SuccessState
import com.example.common_base.web.WebViewActivity
import com.example.module_home.ArticleViewModelFactory
import com.example.module_home.R
import com.example.module_home.composite.bean.Project
import com.example.module_home.databinding.FragmentCompositePageBinding
import com.example.module_home.home.ArticleViewModel

private const val CID = "cid"

/**
 * 综合
 */
class CompositePageFragment : BaseMvvmFragment<FragmentCompositePageBinding, ArticleViewModel>() {

    private var mCurPage: Int = 1
    private var cid: Int = 0
    private lateinit var mAdapter: CompositeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cid = arguments?.getInt(CID) ?: 0
    }

    override fun initData() {
        viewDataBinding.srlComposite.autoRefresh()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRefresh()
        initRecycler()
    }

    private fun initRefresh() {
        viewDataBinding.srlComposite.apply {
            setOnRefreshListener {
                mCurPage = 1
                viewModel.getTreeNode(mCurPage, cid)
            }
            setOnLoadMoreListener {
                viewModel.getTreeNode(mCurPage + 1, cid)
            }
            setEnableLoadMore(true)
            setEnableRefresh(true)
        }
    }

    private fun initRecycler() {
        mAdapter = CompositeAdapter(R.layout.article_rv_item_composite)
        viewDataBinding.rvPage.apply {
            layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
            adapter = mAdapter
        }

        mAdapter.setOnItemClickListener { adapter, _, position ->
            WebViewActivity.launch(requireActivity(), (adapter.data[position] as Project).link)
        }
    }

    override fun addObserver() {
        super.addObserver()
        viewModel.treeNodeData.observe(this) {
            if (mCurPage == 1) {
                mAdapter.setList(it)
            } else {
                mAdapter.addData(it)
            }
        }
        viewModel.mStateLiveData.observe(this) {
            when (it) {
                is SuccessState -> {
                    mCurPage += 1
                    viewDataBinding.srlComposite.finishRefresh()
                    viewDataBinding.srlComposite.finishLoadMore()
                }

                is ErrorState -> {
                    mCurPage = 0
                    viewDataBinding.srlComposite.finishRefresh(false)
                    viewDataBinding.srlComposite.finishLoadMore(false)
                }

                is CompleteState -> {
                    viewDataBinding.srlComposite.finishRefreshWithNoMoreData()
                }

                else -> {
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(cid: Int) =
            CompositePageFragment().apply {
                arguments = Bundle().apply {
                    putInt(CID, cid)
                }
            }
    }

    override fun createViewModel(): ArticleViewModel {
        return ArticleViewModelFactory().create(ArticleViewModel::class.java)
    }

    override fun getLayoutResId(): Int = R.layout.fragment_composite_page

}