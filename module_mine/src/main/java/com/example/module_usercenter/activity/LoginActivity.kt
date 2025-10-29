package com.example.module_usercenter.activity

import android.content.Context
import android.graphics.Paint
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.common_base.base.mvp.BaseMVPActivity
import com.example.common_base.constants.AConstance
import com.example.common_base.util.StatusBarUtil
import com.example.common_base.util.ToastUtil
import com.example.common_base.util.UserHelper
import com.example.module_usercenter.R
import com.example.module_usercenter.bean.LoginResult
import com.example.module_usercenter.contract.LoginContract
import com.example.module_usercenter.event.LoginEvent
import com.example.module_usercenter.presenter.LoginPresenter
import org.greenrobot.eventbus.EventBus

@Route(path = AConstance.ACTIVITY_URL_LOGIN)
class LoginActivity : BaseMVPActivity<LoginPresenter>(), LoginContract.View, View.OnClickListener {

    // 声明所有需要的视图变量，使用 lateinit var 延迟初始化
    private lateinit var ivClose: ImageView
    private lateinit var etLoginUsername: EditText
    private lateinit var etLoginPassword: EditText
    private lateinit var cbLoginPwdVisible: CheckBox
    private lateinit var tvRegister: TextView
    private lateinit var btnLogin: Button

    override fun onClick(v: View?) {
        when (v) {
            // 使用声明的变量
            btnLogin -> {
                login()
            }
            // 使用声明的变量
            tvRegister -> ARouter.getInstance().build(AConstance.ACTIVITY_URL_REGISTER).navigation()

            else -> print("none of the above")
        }
    }

    override fun loginSuccess(loginResult: LoginResult) {
        // 使用声明的变量
        UserHelper.saveUserNamePwd(
            etLoginUsername.text.trim().toString(),
            etLoginPassword.text.trim().toString()
        )
        ARouter.getInstance().build(AConstance.ACTIVITY_URL_MAIN).navigation()
        EventBus.getDefault().post(LoginEvent())
//        FlutterBoost.instance().sendEventToFlutter(FlutterConstance.TO_FLUTTER_EVENT_COOKIE, CookieHelper.getDefCookieMap())
        finish()
    }

    override fun getLayoutResId(): Int = R.layout.mine_activity_login

    override fun createPresenter(): LoginPresenter = LoginPresenter()

    override fun initView() {
        StatusBarUtil.setLightMode(this)

        // --- 1. 使用 findViewById 初始化所有视图变量 ---
        ivClose = findViewById(R.id.iv_close)
        etLoginUsername = findViewById(R.id.et_login_username)
        etLoginPassword = findViewById(R.id.et_login_password)
        cbLoginPwdVisible = findViewById(R.id.cb_login_pwd_visible)
        tvRegister = findViewById(R.id.tv_register)
        btnLogin = findViewById(R.id.btn_login)
        // ---------------------------------------------

        // 使用声明的变量
        ivClose.setOnClickListener { finish() }
    }

    override fun initData() {
        super.initData()

        //设置下划线
        tvRegister.paint.flags = Paint.UNDERLINE_TEXT_FLAG

        //拿取已登录过的账号密码显示
        val userName = UserHelper.getUserName()
        val passWord = UserHelper.getUserPwd()

        etLoginUsername.setText(userName)
        etLoginUsername.setSelection(userName.length)
        etLoginPassword.setText(passWord)
        etLoginPassword.setSelection(passWord.length)

        // 设置点击监听
        btnLogin.setOnClickListener(this)
        tvRegister.setOnClickListener(this)

        //密码框右侧的密码可见不可见按钮
        cbLoginPwdVisible.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                //使用声明的变量
                etLoginPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                //设置光标在文字末尾
                etLoginPassword.setSelection(etLoginPassword.text.toString().length)
            } else {
                etLoginPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                etLoginPassword.setSelection(etLoginPassword.text.toString().length)
            }
        }

        etLoginPassword.setOnEditorActionListener { _, actionId, _ ->
            //监听完成按钮 关闭软键盘 并且开始登录
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 使用声明的变量
                val imm =
                    etLoginPassword.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etLoginPassword.windowToken, 0)
                login()
                true
            }
            false
        }
    }

    private fun login() {
        // 使用声明的变量
        val phone = etLoginUsername.text.trim().toString()
        val pwd = etLoginPassword.text.trim().toString()

        if (TextUtils.isEmpty(phone)) {
            ToastUtil.showShortToast(this, getString(R.string.please_input_username))
            return
        }
        if (TextUtils.isEmpty(pwd)) {
            ToastUtil.showShortToast(this, getString(R.string.please_input_password))
            return
        }
        presenter.login(phone, pwd)
    }
}