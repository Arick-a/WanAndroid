package com.example.module_usercenter.activity

import android.content.Context
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common_base.base.mvp.BaseMVPActivity
import com.example.common_base.constants.AConstance
import com.example.common_base.util.StatusBarUtil
import com.example.module_usercenter.R
import com.example.module_usercenter.contract.RegisterContract
import com.example.module_usercenter.presenter.RegisterPresenter

@Route(path = AConstance.ACTIVITY_URL_REGISTER)
class RegisterActivity : BaseMVPActivity<RegisterPresenter>(), RegisterContract.View {
    override fun getLayoutResId(): Int = R.layout.activity_register

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun initView() {
        StatusBarUtil.setLightMode(this)
        findViewById<View>(R.id.btn_register).setOnClickListener {
            val imm = findViewById<View>(R.id.et_register_username)
                .context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(findViewById<EditText>(R.id.et_register_username).windowToken, 0)

            val username = findViewById<EditText>(R.id.et_register_username).text.toString().trim { it <= ' ' }
            val password = findViewById<EditText>(R.id.et_register_password).text.toString().trim { it <= ' ' }
            val repassword = findViewById<EditText>(R.id.et_register_repassword).text.toString().trim { it <= ' ' }
            presenter.register(username, password, repassword)
        }
        findViewById<View>(R.id.iv_close)
            .setOnClickListener { finish() }
    }

    override fun createPresenter(): RegisterPresenter = RegisterPresenter()

    override fun registSuccess() {
        finish()
    }
}
