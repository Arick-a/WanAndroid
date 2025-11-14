package com.example.module_usercenter.activity

import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common_base.base.BaseActivity
import com.example.common_base.util.StatusBarUtil
import com.example.module_usercenter.R


@Route(path = "")
class ThreeStageSheetActivity : BaseActivity() {

    override fun getLayoutResId(): Int = R.layout.activity_three_stage

    override fun initView() {
        StatusBarUtil.setDarkMode(this, true)
    }

    override fun initData() {
    }
}