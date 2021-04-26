package com.example.feign.feign.rabbitmqimp;

import com.example.feign.feign.common.Constants;
import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.common.NamedThreadFactory;
import com.example.feign.feign.rabbitmqinterface.MessageConsumer;
import com.example.feign.feign.rabbitmqinterface.MessageProcess;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by littlersmall on 16/5/23.
 */
@Slf4j
public class ThreadPoolConsumer<T> {
    private ExecutorService executor;
    private volatile boolean stop = false;
    private final ThreadPoolConsumerBuilder<T> infoHolder;

    /**
     * 构造器
     */
    public static class ThreadPoolConsumerBuilder<T> {
        int threadCount;
        long intervalMils;
        MqManager mqManager;
        String exchange;
        String routingKey;
        String queue;
        String type = "direct";
        MessageProcess<T> messageProcess;

        public ThreadPoolConsumerBuilder<T> setThreadCount(int threadCount) {
            this.threadCount = threadCount;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setIntervalMils(long intervalMils) {
            this.intervalMils = intervalMils;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setMQAccessBuilder(MqManager mqManager) {
            this.mqManager = mqManager;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setExchange(String exchange) {
            this.exchange = exchange;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setRoutingKey(String routingKey) {
            this.routingKey = routingKey;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setQueue(String queue) {
            this.queue = queue;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setType(String type) {
            this.type = type;

            return this;
        }

        public ThreadPoolConsumerBuilder<T> setMessageProcess(MessageProcess<T> messageProcess) {
            this.messageProcess = messageProcess;

            return this;
        }

        public ThreadPoolConsumer<T> build() {
            return new ThreadPoolConsumer<T>(this);
        }
    }

    private ThreadPoolConsumer(ThreadPoolConsumerBuilder<T> threadPoolConsumerBuilder) {
        this.infoHolder = threadPoolConsumerBuilder;
        executor = new ThreadPoolExecutor(threadPoolConsumerBuilder.threadCount, threadPoolConsumerBuilder.threadCount,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024), new NamedThreadFactory("rabbitmq-pool-consumer", false));
    }

    /**
     * 1 构造messageConsumer
     * 2 执行consume
     */
    public void start() throws IOException {
        for (int i = 0; i < infoHolder.threadCount; i++) {
            //1 构造messageConsumer
            final MessageConsumer messageConsumer = infoHolder.mqManager.dequeueMsg(infoHolder.exchange,
                    infoHolder.routingKey, infoHolder.queue, infoHolder.messageProcess, infoHolder.type);

            executor.execute(() -> {
                while (!stop) {
                    try {
                        //2 执行consume
                        DetailRes detailRes = messageConsumer.consume();

                        if (infoHolder.intervalMils > 0) {
                            try {
                                Thread.sleep(infoHolder.intervalMils);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                log.info("interrupt ", e);
                            }
                        }

                        if (!detailRes.isSuccess()) {
                            log.info("run error " + detailRes.getErrMsg());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.info("run exception ", e);
                    }
                }
            });
        }
        // jvm关闭的时候先执行该线程钩子
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        this.stop = true;

        try {
            Thread.sleep(Constants.ONE_SECOND);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
