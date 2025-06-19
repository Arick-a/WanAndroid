package com.example.lib_trace.core;

import android.os.Build;
import android.os.Looper;
import android.os.Trace;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.lib_trace.bean.MethodInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * author : zhang.wenqiang
 * date : 2021/10/13
 */
public class TraceBeat {

    public static boolean openTrace = false;
    private static final LinkedList<Entity> methodList = new LinkedList<>();

    private static final Long MAX_DURATION_MS = 1000L;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void start(String name) {
        if (isOpenTraceMethod()) {
            Trace.beginSection(name);
            Long now = System.currentTimeMillis();
            synchronized (methodList) {
                methodList.add(MethodEntryPool.INSTANCE.obtain(name, now, true, isInMainThread()));
            }
            // 滑动窗口保持收集1000ms内的数据
            while (!methodList.isEmpty() &&
                    now - methodList.getFirst().time > MAX_DURATION_MS) {
                Entity entity = methodList.removeFirst();
                MethodEntryPool.INSTANCE.recycle(entity);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void end(String name) {
        if (isOpenTraceMethod()) {
            Trace.endSection();
            synchronized (methodList) {
                methodList.add(MethodEntryPool.INSTANCE.obtain(name, System.currentTimeMillis(), true, isInMainThread()));
            }
        }
    }

    public static void resetTraceData() {
        synchronized (methodList) {
            methodList.clear();
        }
    }

    public static List<MethodInfo> collectTraceData() {
        List<Entity> methodListCopy = new LinkedList<>();
        synchronized (methodList) {
            for (Entity entity : methodList) {
                try {
                    methodListCopy.add((Entity) entity.clone());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        List<MethodInfo> resultList = new ArrayList<>();
        for (int i = 0; i < methodListCopy.size(); i++) {
            Entity startEntity = methodListCopy.get(i);
            if (!startEntity.isStart) {
                continue;
            }
            startEntity.pos = i;
            Entity endEntity = findEndEntity(methodListCopy,startEntity.name, i + 1);
            if (endEntity != null && endEntity.time - startEntity.time > 0) {
                MethodInfo methodInfo = createMethodInfo(startEntity, endEntity);
                resultList.add(methodInfo);
            }
        }
        return resultList;
    }

    private static Entity findEndEntity(List<Entity> methodList,String name, int startPos) {
        int sameCount = 1;
        for (int i = startPos; i < methodList.size(); i++) {
            Entity endEntity = methodList.get(i);
            if (endEntity.name.equals(name)) {
                if (endEntity.isStart) {
                    sameCount++;
                } else {
                    sameCount--;
                }
                if (sameCount == 0 && !endEntity.isStart) {
                    endEntity.pos = i;
                    return endEntity;
                }
            }
        }
        return null;
    }

    private static MethodInfo createMethodInfo(Entity startEntity, Entity endEntity) {
        return new MethodInfo(startEntity.name,
                endEntity.time - startEntity.time,
                startEntity.pos,
                endEntity.pos,
                startEntity.isMainThread);
    }

    private static boolean isInMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    private static boolean isOpenTraceMethod() {
        return openTrace;
    }

    public static final class Entity implements Cloneable {
        public String name;
        public Long time;
        public boolean isStart;
        public int pos;
        public boolean isMainThread;

        public Entity(String name, Long time, boolean isStart, boolean isMainThread) {
            this.name = name;
            this.time = time;
            this.isStart = isStart;
            this.isMainThread = isMainThread;
        }

        @NonNull
        @Override
        protected Object clone() throws CloneNotSupportedException {
            // 使用父类的 clone 方法，保证克隆对象的基本字段复制
            Entity cloned = (Entity) super.clone();
            // 手动复制引用类型字段
            cloned.name = this.name; // 深拷贝 String
            cloned.time = this.time;   // 深拷贝 Long
            // 布尔和整型字段无需深拷贝，因为它们是基本类型
            return cloned;
        }
    }

    public static final class IndexRecord {
        public IndexRecord(int index) {
            this.index = index;
        }

        public int index;
        public String source;

        @Override
        public String toString() {
            return "index:" + index + ",\t" + " source:" + source;
        }
    }
}
