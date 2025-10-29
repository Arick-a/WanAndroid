package com.example.lib_trace.tracer

import android.os.Handler
import android.os.Looper
import com.example.lib_trace.Trace
import com.example.lib_trace.config.ShareInfo.TRACE_MEMORY_DALVIK
import com.example.lib_trace.config.ShareInfo.TRACE_MEMORY_NATIVE
import com.example.lib_trace.config.ShareInfo.TRACE_MEMORY_VM_SIZE
import com.example.lib_trace.config.ShareInfo.TRACE_TYPE
import com.example.lib_trace.config.ShareInfo.TRACE_TYPE_ANR
import com.example.lib_trace.config.ShareInfo.TRACE_UI_STACK
import com.example.lib_trace.config.ShareInfo.TRACE_UI_THREAD_STATUS
import com.example.lib_trace.listeners.LogReporter
import com.example.lib_trace.listeners.LooperObserver
import com.example.lib_trace.util.DeviceUtil
import com.example.lib_trace.util.LogUtils
import com.example.lib_trace.util.TraceHandlerThread
import com.example.lib_trace.util.Utils
import org.json.JSONObject

/**
 *  author : zhang.wenqiang
 *  date : 2021/10/19
 *  description : Anr监控
 *  1. dispatchBegin 主线程 postDelay 5000 task
 *  2. dispatchEnd remove task
 *  3. task find stack
 */
class AnrTracer(private val reporter: LogReporter? = null) : LooperObserver {

    companion object {
        val TAG = AnrTracer::class.java.simpleName
        val DEFAULT_ANR = 5 * 1000
        val THREAD_ANR = "thread_anr"
    }

    private val anrHandler = Handler(TraceHandlerThread.getNewHandlerThread(THREAD_ANR).looper)
    private val anrTask = AnrTask()

    override fun dispatchBegin(beginNs: Long, first: Long) {
        val cost: Long = System.currentTimeMillis() - first
//        LogUtils.d(TAG, "dispatch begin,cost:%s,delay:%s", cost, DEFAULT_ANR - cost)
        anrHandler.postDelayed(anrTask, DEFAULT_ANR - cost)
    }

    override fun dispatchEnd(beginNs: Long, endNs: Long) {
//        LogUtils.d(TAG, "dispatch end, beginNs:%s,endNs:%s,cost", beginNs, endNs, endNs - beginNs)
        anrHandler.removeCallbacks(anrTask)
    }

    inner class AnrTask : Runnable {
        override fun run() {
            var jsonObject = JSONObject()
            val memory = dumpMemory()
            val status = Looper.getMainLooper().thread.state
            val stackTrace = Looper.getMainLooper().thread.stackTrace
            val wholeStack = Utils.getWholeStack(stackTrace, "|*\t\t")
            jsonObject = DeviceUtil.getDeviceInfo(jsonObject, Trace.sApp)
            jsonObject.put(TRACE_TYPE, TRACE_TYPE_ANR)
            jsonObject.put(TRACE_MEMORY_DALVIK, memory[0])
            jsonObject.put(TRACE_MEMORY_NATIVE, memory[1])
            jsonObject.put(TRACE_MEMORY_VM_SIZE, memory[2])
            jsonObject.put(TRACE_UI_THREAD_STATUS, status)
            jsonObject.put(TRACE_UI_STACK, wholeStack)
            LogUtils.e(TAG, jsonObject.toString())
            reporter?.report(jsonObject.toString())
        }
    }

    private fun dumpMemory(): LongArray {
        val memory = LongArray(3)
        memory[0] = DeviceUtil.getDalvikHeap()
        memory[1] = DeviceUtil.getNativeHeap()
        memory[2] = DeviceUtil.getVmSize()
        return memory
    }
}