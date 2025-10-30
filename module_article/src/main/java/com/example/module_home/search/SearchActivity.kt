package com.example.module_home.search

import android.view.KeyEvent
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.example.common_base.base.mvvm.BaseMvvmActivity
import com.example.common_base.util.StatusBarUtil
import com.example.common_base.widget.SearchableView
import com.example.module_home.ArticleViewModelFactory
import com.example.module_home.BR
import com.example.module_home.R
import com.example.module_home.databinding.ArticleActivitySearchBinding

/**
 * 搜索文章页
 */
class SearchActivity : BaseMvvmActivity<ArticleActivitySearchBinding, SearchViewModel>() {

    override fun initView() {
        StatusBarUtil.setLightMode(this)
        viewDataBinding?.tvCancle?.setOnClickListener { finish() }

        viewDataBinding!!.searchInput.apply {

            setIconifiedByDefault(false)
            queryHint = getString(R.string.str_input_key)

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    search(msg = query)
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })

            setOnCloseClickListener(object : SearchableView.OnCloseClickListener {
                override fun onClose() {
                    selectFragment(0)
                }
            })

            requestFocus()
        }

        fm = supportFragmentManager
        createFragment()
        selectFragment(0)
    }

    fun search(msg: String?, submit: Boolean = false) {
        selectFragment(1)
        viewDataBinding!!.searchInput.clearFocus()
        viewModel.searchKey.value = msg
        if (submit) {
            viewDataBinding!!.searchInput.setQuery(msg, false)
        }
    }

    private lateinit var fm: FragmentManager
    private var fragments = mutableListOf<Fragment>()

    private fun selectFragment(index: Int) {
        val ft = fm.beginTransaction()
        for (i in fragments.indices) {
            if (i == index) {
                ft.show(fragments[i])
            } else {
                ft.hide(fragments[i])
            }
        }
        ft.commit()
    }

    private fun createFragment() {
        val transaction = fm.beginTransaction()

        val hotKeyFragment = HotKeyFragment.newInstance()
        val searchResultFragment = SearchResultFragment.newInstance()

        transaction.apply {
            add(R.id.fl_content, hotKeyFragment)
            add(R.id.fl_content, searchResultFragment)
        }

        fragments.apply {
            add(hotKeyFragment)
            add(searchResultFragment)
        }

        transaction.commit()
    }

    override fun initData() {
        viewModel.getHotKey()
        viewModel.getAllHistory()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!fragments[0].isVisible) {
                viewDataBinding!!.searchInput.setQuery("", false)
                selectFragment(0)
                return true
            }
            return super.onKeyDown(keyCode, event)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun getLayoutResId(): Int = R.layout.article_activity_search

    override fun initVariableId(): Int = BR.viewModel

    override fun createViewModel(): SearchViewModel {
        return ViewModelProvider(this, ArticleViewModelFactory()).get(SearchViewModel::class.java)
    }
}