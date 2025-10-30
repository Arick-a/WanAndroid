package com.example.module_home.home

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.example.common_base.base.mvvm.BaseMvvmFragment
import com.example.common_base.base.viewmodel.ErrorState
import com.example.common_base.base.viewmodel.SuccessState
import com.example.common_base.constants.FlutterConstance
import com.example.common_base.constants.FlutterConstance.FLUTTER_EVENT_TYPE
import com.example.common_base.constants.FlutterConstance.FLUTTER_TYPE_LOGIN_OUT
import com.example.common_base.moudle_service.UserInfoService
import com.example.common_base.performance.TIME_MONITOR_APP_ONCREATE
import com.example.common_base.performance.TimeMonitorManager
import com.example.common_base.util.CookieHelper
import com.example.common_base.util.ToastUtil
import com.example.common_base.util.UserHelper
import com.example.common_base.web.WebViewActivity
import com.example.common_base.widget.LinearItemDecoration
import com.example.module_home.ArticleViewModelFactory
import com.example.module_home.R
import com.example.module_home.databinding.FragmentFirstPageBinding
import com.example.module_home.home.adapter.RecommendAdapter
import com.example.module_home.home.bean.Article
//import com.idlefish.flutterboost.FlutterBoost
//import com.idlefish.flutterboost.ListenerRemover

/**
 * 文章model首页
 */
class HomeFragment : BaseMvvmFragment<FragmentFirstPageBinding, ArticleViewModel>() {

    private lateinit var mAdapter: RecommendAdapter
//    private lateinit var collectListener: ListenerRemover
//    private lateinit var loginListener: ListenerRemover

    @JvmField
    @Autowired
    var userService: UserInfoService? = null

    override fun createViewModel(): ArticleViewModel {
        return ArticleViewModelFactory().create(ArticleViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        ARouter.getInstance().navigation(UserInfoService::class.java)
        initRecycler()
        initRefresh()
//        collectListener = FlutterBoost.instance()
//            .addEventListener(FlutterConstance.FROM_FLUTTER_EVENT_COLLECT) { _, _ ->
//                viewModel.getArticles(isRefresh = true)
//            }
//        loginListener = FlutterBoost.instance()
//            .addEventListener(FlutterConstance.FROM_FLUTTER_EVENT_LOGIN) { _, args ->
//                if (args[FLUTTER_EVENT_TYPE] == FLUTTER_TYPE_LOGIN_OUT) {
//                    CookieHelper.clearCookie()
//                    viewModel.getArticles(isRefresh = true)
//                }
//            }
        TimeMonitorManager.getTimeMonitor(TIME_MONITOR_APP_ONCREATE)
            .recordingTimeTag("HomeFragment-onActivityCreated-end")
    }

    private fun initRefresh() {
        viewDataBinding.srlRefresh.apply {
            setOnRefreshListener { viewModel.getArticles(isRefresh = true) }
            setOnLoadMoreListener { viewModel.getArticles() }
            setEnableLoadMore(true)
            setEnableRefresh(true)
            autoRefresh()
        }
    }

    private fun initRecycler() {
        mAdapter = RecommendAdapter(R.layout.item_home_recycler).apply {
            addChildClickViewIds(R.id.iv_home_like)
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.iv_home_like) {
                    if (userService?.isLogged == false) {
                        ToastUtil.showShortToast(
                            requireContext(),
                            getString(R.string.common_login_first)
                        )
                        return@setOnItemChildClickListener
                    }
                    val article = mAdapter.data[position]
                    article.collect = !article.collect
                    mAdapter.setData(position, article)
                    viewModel.changeCollect(article.collect, article.id)
                }
            }

            setOnItemClickListener { adapter, _, position ->
                WebViewActivity.launch(requireActivity(), (adapter.data[position] as Article).link)
            }
        }
        viewDataBinding.rvContent.apply {
            addItemDecoration(
                LinearItemDecoration(requireContext()).color(
                    ContextCompat.getColor(requireContext(), R.color.gray_ea)
                )
                    .height(1f)
                    .margin(15f, 15f)
            )
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mAdapter
        }
    }

    override fun addObserver() {
        super.addObserver()
        viewModel.articleData.observe(viewLifecycleOwner) {
            mAdapter.setData(it)
        }

        viewModel.mStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is SuccessState -> {
                    viewDataBinding.srlRefresh.finishRefresh()
                    viewDataBinding.srlRefresh.finishLoadMore()
                }
                is ErrorState -> {
                    viewDataBinding.srlRefresh.finishRefresh(false)
                    viewDataBinding.srlRefresh.finishLoadMore(false)
                    ToastUtil.showShortToast(requireContext(), it.message)
                }
                else -> {
                }
            }
        }
    }

    override fun getLayoutResId(): Int = R.layout.fragment_first_page

    override fun onDestroy() {
        super.onDestroy()
//        collectListener.remove()
//        loginListener.remove()
    }
}