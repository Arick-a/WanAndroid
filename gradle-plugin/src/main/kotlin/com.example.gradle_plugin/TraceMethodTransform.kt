package com.example.gradle_plugin

import TraceParameters
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassData
import com.example.gradle_plugin.trace.Config
import com.example.gradle_plugin.trace.TraceClassVisitor
import org.objectweb.asm.ClassVisitor
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

abstract class TraceMethodTransform : AsmClassVisitorFactory<TraceParameters> {

    companion object {
        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        private val logFile = File(System.getProperty("user.dir"), "instrumented_classes.log")

        private fun logToFile(message: String) {
            synchronized(logFile) {
                PrintWriter(FileWriter(logFile, true)).use { pw ->
                    pw.println(message)
                }
            }
        }
    }

    override fun createClassVisitor(
        classContext: com.android.build.api.instrumentation.ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val params = parameters.get()
        val config = Config().apply {
            mIsNeedLogTraceInfo = params.logTraceInfo.get()
            mBeatClass = params.beatClass.orNull
            mNeedTracePackageMap.addAll(params.needTracePackages.get())
            mWhitePackageMap.addAll(params.whitePackages.get())
            mWhiteClassMap.addAll(params.whiteClasses.get())
        }
        return TraceClassVisitor(nextClassVisitor, config)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val params = parameters.get()
        val className = classData.className.replace('/', '.')
        val beatClass = params.beatClass.orNull?.replace('/', '.') ?: ""

        val needTracePackages = params.needTracePackages.get()
        val whitePackages = params.whitePackages.get()
        val whiteClasses = params.whiteClasses.get()

        val reasons = mutableListOf<String>()

        if (!params.open.get()) {
            reasons.add("open=false")
        }
        if (className == beatClass) {
            reasons.add("is beatClass itself")
        }
        if (className.contains("R\$") || className.endsWith("R")) {
            reasons.add("R class")
        }
        if (className.contains("BuildConfig")) {
            reasons.add("BuildConfig")
        }
        if (className.contains("Manifest")) {
            reasons.add("Manifest")
        }
        if (needTracePackages.isNotEmpty() && needTracePackages.none { className.contains(it.replace('/', '.')) }) {
            reasons.add("not in needTracePackages")
        }
        if (whitePackages.any { className.contains(it.replace('/', '.')) }) {
            reasons.add("in whitePackages")
        }
        if (whiteClasses.any { className == it.replace('/', '.') }) {
            reasons.add("in whiteClasses")
        }

        val instrumentable = reasons.isEmpty()

        // 写入文件
        val now = sdf.format(Date())
        val line = "$now | $className | instrumentable=$instrumentable | reasons=${reasons.joinToString(";")}"
//        logToFile(line)

        // 控制台打印
        if (params.logTraceInfo.get()) {
            println(line)
        }

        return instrumentable
    }
}
