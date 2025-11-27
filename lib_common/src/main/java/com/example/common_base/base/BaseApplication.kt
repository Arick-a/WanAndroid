package com.example.common_base.base

//import com.idlefish.flutterboost.FlutterBoost
//import com.idlefish.flutterboost.FlutterBoostDelegate
//import com.idlefish.flutterboost.FlutterBoostRouteOptions
//import com.idlefish.flutterboost.containers.FlutterBoostActivity
//import io.flutter.embedding.android.FlutterActivityLaunchConfigs
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import com.alibaba.android.arouter.launcher.ARouter
import com.example.common_base.BuildConfig
import com.example.common_base.R
import com.example.common_base.hotfix.HotFix
import com.example.common_base.performance.TIME_MONITOR_APP_ONCREATE
import com.example.common_base.performance.TimeMonitorManager
import com.example.common_base.util.CrashHandler
import com.example.common_base.util.GlobalThreadPools
import com.example.common_base.web.CommonWebView
import com.example.common_base.widget.refresh.ClassicsHeader
import com.example.lib_trace.Trace
import com.example.lib_trace.listeners.LogReporter
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.layout.SmartRefreshLayout


/**
 * Description:
 */
open class BaseApplication : Application() {

    companion object {
        lateinit var sApplication: Application
    }

    init {
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            layout.setPrimaryColorsId(R.color.trans_10, R.color.black_999)
            ClassicsHeader(context)
        }

        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, _ ->
            ClassicsFooter(context).setDrawableSize(20f)
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        TimeMonitorManager.resetTimeMonitor(TIME_MONITOR_APP_ONCREATE)
    }

    override fun onCreate() {
        super.onCreate()
        sApplication = this
        CrashHandler.getInstance().init(applicationContext)
        CommonWebView.preloadWebView(this)

        val trace = Trace.Builder(this)
            .logReporter(EvilLogReporter())
            .enableEvil(enable = true)
            .enableAnr(enable = true)
            .build()
        trace.start()

        GlobalThreadPools.execute {
            initArouter()
            HotFix.init(this)
        }

        initFlutter()

//        TimeMonitorManager.initBlock(applicationContext)
        TimeMonitorManager.getTimeMonitor(TIME_MONITOR_APP_ONCREATE)
            .recordingTimeTag("aplication-onCreate-end")
    }

    private class EvilLogReporter : LogReporter {
        override fun report(json: String) {

        }
    }

    private fun initFlutter() {
//        FlutterBoost.instance().setup(this, object : FlutterBoostDelegate {
//            override fun pushNativeRoute(options: FlutterBoostRouteOptions) {
//                if (options.pageName().equals(FLUTTER_PAGE_WEB)) {
//                    val intent = Intent(
//                        FlutterBoost.instance().currentActivity(),
//                        WebViewActivity::class.java
//                    )
//                    intent.putExtra(URL, options.arguments()["url"].toString())
//                    FlutterBoost.instance().currentActivity()
//                        .startActivityForResult(intent, options.requestCode())
//                }
//            }
//
//            override fun pushFlutterRoute(options: FlutterBoostRouteOptions) {
//                val intent: Intent = FlutterBoostActivity.CachedEngineIntentBuilder(
//                    FlutterBoostActivity::class.java
//                )
//                    .backgroundMode(FlutterActivityLaunchConfigs.BackgroundMode.transparent)
//                    .destroyEngineWithActivity(false)
//                    .uniqueId(options.uniqueId())
//                    .url(options.pageName())
//                    .urlParams(options.arguments())
//                    .build(FlutterBoost.instance().currentActivity())
//                FlutterBoost.instance().currentActivity().startActivity(intent)
//            }
//        }) {}
    }

    private fun initArouter() {
        try {
            if (isDebug()) {
                ARouter.openLog()
                ARouter.openDebug()
                ARouter.printStackTrace()
            }
            ARouter.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
            //异常后建议清除映射表官方文档说 开发模式会清除
            ARouter.openDebug()
            ARouter.init(this)
        }
    }

    private fun isDebug(): Boolean = BuildConfig.DEBUG
}