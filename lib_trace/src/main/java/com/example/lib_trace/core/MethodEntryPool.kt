package com.example.lib_trace.core

object MethodEntryPool {
    private val pool = ArrayDeque<TraceBeat.Entity>(1024)

    fun obtain(name: String, time: Long, isStart: Boolean, isMainThread: Boolean): TraceBeat.Entity {
        return if (pool.isNotEmpty()) {
            val entry = pool.removeFirst()
            entry.name = name
            entry.time = time
            entry.isStart = isStart
            entry.isMainThread = isMainThread
            entry
        } else {
            TraceBeat.Entity(name,time, isStart, isMainThread)
        }
    }

    fun recycle(entry: TraceBeat.Entity) {
        pool.addLast(entry)
    }
}