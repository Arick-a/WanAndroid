package com.example.gradle_plugin.trace

import com.example.gradle_plugin.util.Utils
import java.io.File
import java.io.FileNotFoundException
import java.util.*

/**
 * author : zhang.wenqiang
 * date : 2021/10/13
 * description :
 */
open class Config {
    private val UNNEED_TRACE_CLASS = arrayOf("R.class", "R$", "Manifest", "BuildConfig")

    //插桩代码所在类
    var mBeatClass: String? = null

    //插桩配置文件
    var mTraceConfigFile: String? = null

    //是否需要打印出所有被插桩的类和方法
    var mIsNeedLogTraceInfo = false

    //在需插桩的包范围内的 无需插桩的包名
    val mWhitePackageMap: HashSet<String> by lazy {
        HashSet<String>()
    }

    //需插桩的包
    val mNeedTracePackageMap: HashSet<String> by lazy {
        HashSet<String>()
    }

    //在需插桩的包范围内的 无需插桩的白名单
    val mWhiteClassMap: HashSet<String> by lazy {
        HashSet<String>()
    }

    fun isNeedTraceClass(fileName: String): Boolean {
        var isNeed = true
        if (fileName.endsWith(".class")) {
            for (unTraceCls in UNNEED_TRACE_CLASS) {
                if (fileName.contains(unTraceCls)) {
                    isNeed = false
                    break
                }
            }
        } else {
            isNeed = false
        }
        return isNeed
    }

    //判断是否是traceConfig.txt中配置范围的类
    fun isConfigTraceClass(className: String): Boolean {

        fun isInNeedTracePackage(): Boolean {
            var isIn = false
            mNeedTracePackageMap.forEach {
                if (className.contains(it)) {
                    isIn = true
                    return@forEach
                }

            }
            return isIn
        }

        fun isInWhitePackage(): Boolean {
            var isIn = false
            mWhitePackageMap.forEach {
                if (className.contains(it)) {
                    isIn = true
                    return@forEach
                }

            }
            return isIn
        }

        fun isInWhiteClass(): Boolean {
            var isIn = false
            mWhiteClassMap.forEach {
                if (className == it) {
                    isIn = true
                    return@forEach
                }

            }
            return isIn
        }

        return if (mNeedTracePackageMap.isEmpty()) {
            !(isInWhitePackage() || isInWhiteClass())
        } else {
            if (isInNeedTracePackage()) {
                !(isInWhitePackage() || isInWhiteClass())
            } else {
                false
            }
        }
    }

    /**
     * 解析插桩配置文件
     */
    fun parseTraceConfigFile() {
        println("parseTraceConfigFile start!!!!!!!!!!!!")
        val traceConfigFile = File(mTraceConfigFile)
        if (!traceConfigFile.exists()) {
            throw FileNotFoundException(
                """
                    找不到 $mTraceConfigFile 配置文件。
                """.trimIndent()
            )
        }

        val configStr = Utils.readFileAsString(traceConfigFile.absolutePath)

        val configArray =
            configStr.split(System.lineSeparator().toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()

        for (element in configArray) {
            var config = element
            if (config.isNullOrBlank()) {
                continue
            }
            if (config.startsWith("#")) {
                continue
            }
            if (config.startsWith("[")) {
                continue
            }

            when {
                config.startsWith("-tracepackage ") -> {
                    config = config.replace("-tracepackage ", "")
                    mNeedTracePackageMap.add(config)
                    println("tracepackage:$config")
                }
                config.startsWith("-keepclass ") -> {
                    config = config.replace("-keepclass ", "")
                    mWhiteClassMap.add(config)
                    println("keepclass:$config")
                }
                config.startsWith("-keeppackage ") -> {
                    config = config.replace("-keeppackage ", "")
                    mWhitePackageMap.add(config)
                    println("keeppackage:$config")
                }
                config.startsWith("-beatclass ") -> {
                    config = config.replace("-beatclass ", "")
                    mBeatClass = config
                    println("beatclass:$config")
                }
                else -> {
                }
            }
        }
    }
}