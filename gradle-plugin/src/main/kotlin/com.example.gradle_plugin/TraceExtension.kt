package com.example.gradle_plugin

import org.gradle.api.provider.Property

abstract class TraceExtension {

    // val output: String = "" -> abstract val output: Property<String>
    abstract val output: Property<String>

    // boolean open -> abstract val open: Property<Boolean>
    abstract val open: Property<Boolean>

    // String traceConfigFile -> abstract val traceConfigFile: Property<String>
    abstract val traceConfigFile: Property<String>

    // boolean logTraceInfo -> abstract val logTraceInfo: Property<Boolean>
    abstract val logTraceInfo: Property<Boolean>

    init {
        // 设置默认值 (Convention)
        output.convention("")
        open.convention(true)
        traceConfigFile.convention("")
        logTraceInfo.convention(false)
    }
}