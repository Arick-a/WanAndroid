package com.example.module_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.common_base.base.BaseApplication
import com.example.module_home.data.DataProvider
import com.example.module_home.data.source.RemoteDataSource
import com.example.module_home.home.ArticleViewModel
import com.example.module_home.search.SearchViewModel

/**
 *  @author : zhang.wenqiang
 *  @date : 2020/12/1
 *  description :
 */
class ArticleViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = with(modelClass) {
        when {
            isAssignableFrom(ArticleViewModel::class.java) ->
                ArticleViewModel(RemoteDataSource())
            isAssignableFrom(SearchViewModel::class.java) ->
                SearchViewModel(DataProvider.provideRepository(BaseApplication.sApplication.applicationContext))
            else ->
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T
}