package com.example.module_usercenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.module_usercenter.api.DefMineRepository
import com.example.module_usercenter.api.MineRepository

/**
 *  @author : zhang.wenqiang
 *  @date : 2020/12/1
 *  description :
 */
class MineViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>) = with(modelClass) {
        when {
            isAssignableFrom(MineViewModel::class.java) ->
                MineViewModel(DefMineRepository())
            else ->
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    } as T
}