package com.example.feign.feign.example;

import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.rabbitmqimp.MqManager;
import com.example.feign.feign.rabbitmqinterface.MessageConsumer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by littlersmall on 16/6/28.
 */
@Service
public class ConsumerExample {
    private static final String EXCHANGE = "example";
    private static final String ROUTING = "user-example";
    private static final String QUEUE = "user-example";

    @Autowired
    ConnectionFactory connectionFactory;

    private MessageConsumer messageConsumer;

    @PostConstruct
    public void init() throws IOException {
        MqManager mqManager = new MqManager(connectionFactory);
        messageConsumer = mqManager.dequeueMsg(EXCHANGE, ROUTING, QUEUE, new UserMessageProcess(), "direct");
    }

    public DetailRes consume() {
        return messageConsumer.consume();
    }

    public String dequeue() {
        return messageConsumer.dequeue();
    }

    public static void main(String[] args) throws IOException {
        ApplicationContext ac = new ClassPathXmlApplicationContext("applicationContext.xml");
        ConsumerExample consumerExample = ac.getBean(ConsumerExample.class);

        consumerExample.consume();
    }
}
