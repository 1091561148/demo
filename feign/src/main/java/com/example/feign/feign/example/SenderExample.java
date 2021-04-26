package com.example.feign.feign.example;

import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.rabbitmqimp.MqManager;
import com.example.feign.feign.rabbitmqinterface.MessageSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by littlersmall on 16/6/28.
 */
@Slf4j
@Service
public class SenderExample {
    private static final String EXCHANGE = "example";
    private static final String ROUTING = "user-example";
    private static final String QUEUE = "user-example";

    @Autowired
    ConnectionFactory connectionFactory;

    private MessageSender messageSender;

    @PostConstruct
    public void init() {
        MqManager mqManager = new MqManager(connectionFactory);
        try {
            messageSender = mqManager.buildMessageSender(EXCHANGE, ROUTING, QUEUE);
        } catch (IOException e) {
            log.error("can not get connection:" + EXCHANGE + ROUTING + QUEUE);
        }
    }

    public DetailRes send(UserMessage userMessage) {
        return messageSender.send(userMessage);
    }
}
