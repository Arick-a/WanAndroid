package com.example.gradle_plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.example.gradle_plugin.trace.Config
import org.gradle.api.Plugin
import org.gradle.api.Project

class TracePlugin  : Plugin<Project> {
    override fun apply(project: Project) {
        println("-------------------- TracePlugin apply begin ---------------------")
        // 读一下build.gradle中的配置 以及 解析配置文件
        val extension = project.extensions.create("traceConfig", TraceExtension::class.java)

        //这里appExtension获取方式与原transform api不同，可自行对比
        val appExtension = project.extensions.getByType(
            AndroidComponentsExtension::class.java
        )
        //这里通过transformClassesWith替换了原registerTransform来注册字节码转换操作
        appExtension.onVariants { variant ->
            //可以通过variant来获取当前编译环境的一些信息，最重要的是可以 variant.name 来区分是debug模式还是release模式编译
            variant.instrumentation.transformClassesWith(
                TraceMethodTransform::class.java,
                InstrumentationScope.ALL
            ) { params ->
                val config = Config().apply {
                    mTraceConfigFile = extension.traceConfigFile.get()
                }
                config.parseTraceConfigFile()
                params.open.set(extension.open)
                params.logTraceInfo.set(extension.logTraceInfo)
                params.traceConfigFile.set(extension.traceConfigFile.get())
                params.beatClass.set(config.mBeatClass ?: "")
                params.needTracePackages.set(config.mNeedTracePackageMap)
                params.whitePackages.set(config.mWhitePackageMap)
                params.whiteClasses.set(config.mWhiteClassMap)
            }
            //InstrumentationScope.ALL 配合 FramesComputationMode.COPY_FRAMES可以指定该字节码转换器在全局生效，包括第三方lib
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }
    }

}