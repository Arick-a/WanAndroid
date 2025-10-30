package com.example.lib_trace.core;

import java.util.ArrayDeque;
import java.util.Deque;

public class MethodEntryPool {

    // 1. 饿汉式：在类加载时就创建唯一的实例
    private static final MethodEntryPool INSTANCE = new MethodEntryPool();

    // 2. 容器不再是静态的，而是实例的字段
    private final Deque<TraceBeat.Entity> pool = new ArrayDeque<>(1024);

    /**
     * 3. 私有构造函数，防止外部直接 new 实例
     */
    private MethodEntryPool() {
        // 可以执行一些初始化操作
    }

    /**
     * 4. 公共静态方法，获取唯一的 MethodEntryPool 实例
     */
    public static MethodEntryPool getInstance() {
        return INSTANCE;
    }

    /**
     * 从对象池获取一个 Entity 对象。
     */
    public TraceBeat.Entity obtain(
            String name,
            long time,
            boolean isStart,
            boolean isMainThread
    ) {
        // 逻辑保持不变，但方法不再是静态的
        if (!pool.isEmpty()) {
            // 从池中取出并重置字段
            TraceBeat.Entity entry = pool.removeFirst();
            // 重新设置字段以复用对象
            entry.name = name;
            entry.time = time;
            entry.isStart = isStart;
            entry.isMainThread = isMainThread;
            return entry;
        } else {
            // 池为空，创建新对象
            return new TraceBeat.Entity(name, time, isStart, isMainThread);
        }
    }


    /**
     * 回收 Entity 对象到池中。
     *
     * @param entry 需要回收的 Entity 实例
     */
    public void recycle(TraceBeat.Entity entry) {
        // 逻辑保持不变，但方法不再是静态的
        pool.addLast(entry);
    }
}