package com.example.feign.feign.common;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final ThreadGroup group;
    private final AtomicInteger threadNumber;
    private final boolean isDeamon;
    private final Thread.UncaughtExceptionHandler handler;

    public NamedThreadFactory(String prefix, boolean isDeamon) {
        this(prefix, null, isDeamon);
    }

    public NamedThreadFactory(String prefix, ThreadGroup threadGroup, boolean isDeamon) {
        this(prefix, threadGroup, isDeamon, null);
    }

    public NamedThreadFactory(String prefix, ThreadGroup threadGroup, boolean isDeamon, Thread.UncaughtExceptionHandler handler) {
        this.threadNumber = new AtomicInteger(1);
        this.prefix = prefix;
        if (null == threadGroup) {
            threadGroup = Thread.currentThread().getThreadGroup();
        }

        this.group = threadGroup;
        this.isDeamon = isDeamon;
        this.handler = handler;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(this.group, r, String.format("%s%d", this.prefix, this.threadNumber.getAndIncrement()));
        if (this.isDeamon) {
            t.setDaemon(true);
        }

        if (null != this.handler) {
            t.setUncaughtExceptionHandler(this.handler);
        }

        if (5 != t.getPriority()) {
            t.setPriority(5);
        }

        return t;
    }
}
