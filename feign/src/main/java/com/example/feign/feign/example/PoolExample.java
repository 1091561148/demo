package com.example.feign.feign.example;

import com.example.feign.feign.common.Constants;
import com.example.feign.feign.rabbitmqimp.MqManager;
import com.example.feign.feign.rabbitmqimp.ThreadPoolConsumer;
import com.example.feign.feign.rabbitmqinterface.MessageProcess;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by littlersmall on 16/6/28.
 */
@Service
public class PoolExample {
    private static final String EXCHANGE = "example";
    private static final String ROUTING = "user-example";
    private static final String QUEUE = "user-example";

    @Autowired
    ConnectionFactory connectionFactory;

    private ThreadPoolConsumer<UserMessage> threadPoolConsumer;

    @PostConstruct
    public void init() {
        MqManager mqManager = new MqManager(connectionFactory);
        MessageProcess<UserMessage> messageProcess = new UserMessageProcess();

        threadPoolConsumer = new ThreadPoolConsumer.ThreadPoolConsumerBuilder<UserMessage>()
                .setThreadCount(Constants.THREAD_COUNT).setIntervalMils(Constants.INTERVAL_MILS)
                .setExchange(EXCHANGE).setRoutingKey(ROUTING).setQueue(QUEUE)
                .setMQAccessBuilder(mqManager).setMessageProcess(messageProcess)
                .build();
    }

    public void start() throws IOException {
        threadPoolConsumer.start();
    }

    public void stop() {
        threadPoolConsumer.stop();
    }

}
