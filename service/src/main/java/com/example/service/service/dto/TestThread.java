package com.example.service.service.dto;

import java.util.concurrent.locks.ReentrantLock;

public class TestThread implements Runnable {
    private ReentrantLock lock = new ReentrantLock();
    private final Object object;
    private String name;

    TestThread(Object object) {
        this.object = object;
    }

    TestThread(Object object, String name) {
        this.object = object;
        this.name = name;
    }

    @Override
    public void run() {
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Test thread is interrupted");
                break;
            }
            try {
                if (lock.tryLock()) {
                    if (object instanceof Integer) {
                        for (int i = 0; i < (Integer) object; i++) {
                            System.out.println("Thread:" + Thread.currentThread().getName() + "--" + name + ":" + i);
                        }
                    }
                }
            } finally {
                lock.unlock();
            }

//            synchronized(object) {
//                try {
//                    System.out.println("Test thread is wait");
//                    object.wait();
//                } catch (InterruptedException e) {
//                    System.out.println("Test thread is sleeping");
//                    //sleep 清除中断 标志，重新设置
//                    Thread.currentThread().interrupt();
//                }
//            }

            System.out.println("Thread:" + Thread.currentThread().getName() + "--" + "test run test end");
            break;
        }
    }
}
