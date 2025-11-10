package com.example.module_usercenter.activity

import android.animation.Animator
import com.airbnb.lottie.LottieAnimationView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.example.common_base.base.BaseActivity
import com.example.common_base.constants.AConstance
import com.example.common_base.util.YearUtil
import com.example.common_base.util.YearUtil.CLASS_2016
import com.example.module_usercenter.R
import kotlin.random.Random

@Route(path = AConstance.ACTIVITY_URL_SPLASH)
class SplashActivity : BaseActivity() {
    override fun getLayoutResId(): Int = R.layout.activity_splash

    override fun initView() {
//        if (Random.nextBoolean()) {
//            playAnimation()
//        } else {
            toMainPage()
//        }
    }

    private fun playAnimation() {
        val animate = findViewById<LottieAnimationView>(R.id.animate_view)
        animate.apply {
            setAnimation("c.json")
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    toMainPage()
                    overridePendingTransition(R.anim.translate_in, R.anim.translate_out)
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
            playAnimation()
        }
    }

    private fun toMainPage() {
        ARouter.getInstance().build(AConstance.ACTIVITY_URL_MAIN).navigation()
        finish()
    }

    override fun initData() {

    }
}
