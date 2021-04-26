package com.example.feign.feign.rabbitmqimp;

import com.example.feign.feign.common.Constants;
import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.common.MessageWithTime;
import com.example.feign.feign.rabbitmqinterface.MessageConsumer;
import com.example.feign.feign.rabbitmqinterface.MessageProcess;
import com.example.feign.feign.rabbitmqinterface.MessageSender;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by littlersmall on 16/5/11.
 */
@Slf4j
public class MqManager {
    private ConnectionFactory connectionFactory;
    private final static String TYPE_DIRECT = "direct";
    private final static String TYPE_TOPIC = "topic";

    public MqManager(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public MessageSender buildMessageSender(final String exchange, final String routingKey, final String queue) throws IOException {
        return buildMessageSender(exchange, routingKey, queue, TYPE_DIRECT);
    }

    public MessageSender buildTopicMessageSender(final String exchange, final String routingKey) throws IOException {
        return buildMessageSender(exchange, routingKey, null, TYPE_TOPIC);
    }

    /**
     * //1 构造template, exchange, routingkey等
     * //2 设置message序列化方法
     * //3 设置发送确认
     * //4 构造sender方法
     */
    public MessageSender buildMessageSender(final String exchange, final String routingKey,
                                            final String queue, final String type) throws IOException {
        Connection connection = connectionFactory.createConnection();
        //1 构造template, exchange, routingkey等
        if (TYPE_DIRECT.equals(type)) {
            //buildQueue(exchange, routingKey, queue, connection, TYPE_DIRECT);
        } else if (TYPE_TOPIC.equals(type)) {
            buildTopic(exchange, connection);
        }

        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        //确保消息发送失败后可以重新返回到队列中
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setExchange(exchange);
        rabbitTemplate.setRoutingKey(routingKey);
        //2 设置message序列化方法
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        RetryCache retryCache = new RetryCache();
        //3 设置发送确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.info("send message failed confirm: " + cause + correlationData.toString());
            } else {
                if (correlationData != null) {
                    log.info("send message success confirm: correlationData={} ,ack={}", correlationData.getId(), ack);
                    retryCache.del(Long.valueOf(correlationData.getId()));
                } else {
                    log.info("send message success confirm");
                }
            }
        });

        rabbitTemplate.setReturnCallback((var1, var2, var3, var4, var5) -> {
            try {
                Thread.sleep(Constants.ONE_SECOND);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("send message failed return:replyCode={} replyText={} message={} exchange={} routingKey={}"
                    , var2, var1, var3, var4, var5);
//            log.info("send message failed return:replyCode={} replyText={} message={} exchange={} routingKey={}"
//                    ,message.getReplyCode(),message.getReplyText(),message.getMessage(),message.getExchange(),message.getRoutingKey());
            //rabbitTemplate.send(message);
        });

        //4 构造sender方法
        return new MessageSender() {
            {
                retryCache.setSender(this);
            }

            @Override
            public DetailRes send(Object message) {
                long id = retryCache.generateId();
                long time = System.currentTimeMillis();

                return send(new MessageWithTime(id, time, message));
            }

            @Override
            public DetailRes send(MessageWithTime messageWithTime) {
                try {
                    retryCache.add(messageWithTime);
                    rabbitTemplate.correlationConvertAndSend(messageWithTime.getMessage(),
                            new CorrelationData(String.valueOf(messageWithTime.getId())));
                } catch (Exception e) {
                    return new DetailRes(false, "mq message send failed");
                }

                return new DetailRes(true, "mq messaage send success");
            }
        };
    }


    public <T> MessageConsumer dequeueMsg(String exchange, String routingKey, final String queue,
                                          final MessageProcess<T> messageProcess) throws IOException {
        return dequeueMsg(exchange, routingKey, queue, messageProcess, TYPE_DIRECT);
    }

    public <T> MessageConsumer buildTopicMessageConsumer(String exchange, String routingKey, final String queue,
                                                         final MessageProcess<T> messageProcess) throws IOException {
        return dequeueMsg(exchange, routingKey, queue, messageProcess, TYPE_TOPIC);
    }

    /**
     * 1 创建连接和channel
     * 2 设置message序列化方法
     * 3 consume
     */
    public <T> MessageConsumer dequeueMsg(String exchange, String routingKey, final String queue,
                                          final MessageProcess<T> messageProcess, String type) throws IOException {
        final Connection connection = connectionFactory.createConnection();

        //1 创建连接和channel
        buildQueue(exchange, routingKey, queue, connection, type);

        //2 设置message序列化方法
        final MessagePropertiesConverter messagePropertiesConverter = new DefaultMessagePropertiesConverter();
        final MessageConverter messageConverter = new Jackson2JsonMessageConverter();

        //3 consume
        return new MessageConsumer() {
            Channel channel;

            {
                channel = connection.createChannel(false);
            }

            @Override
            public DetailRes consume() {
                try {
                    //1 通过basicGet获取原始数据
                    GetResponse response = channel.basicGet(queue, false);

                    while (response == null) {
                        response = channel.basicGet(queue, false);
                        Thread.sleep(Constants.ONE_SECOND);
                    }

                    Message message = new Message(response.getBody(),
                            messagePropertiesConverter.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8"));

                    //2 将原始数据转换为特定类型的包
                    @SuppressWarnings("unchecked")
                    T messageBean = (T) messageConverter.fromMessage(message);

                    //3 处理数据
                    DetailRes detailRes;

                    try {
                        detailRes = messageProcess.process(messageBean);
                    } catch (Exception e) {
                        log.error("exception", e);
                        detailRes = new DetailRes(false, "process exception: " + e);
                    }

                    //4 手动发送ack确认
                    if (detailRes.isSuccess()) {
                        channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                    } else {
                        //避免过多失败log
                        Thread.sleep(Constants.ONE_SECOND);
                        log.info("process message failed: " + detailRes.getErrMsg());
                        // 消费失败，消息重返队列队首 第二个参数是否应用于多消息，第三个参数是否requeue
                        //true则重新入队列，否则丢弃或者进入死信队列
                        channel.basicNack(response.getEnvelope().getDeliveryTag(), false, true);
                    }

                    return detailRes;
                } catch (InterruptedException e) {
                    log.error("exception", e);
                    return new DetailRes(false, "interrupted exception " + e.toString());
                } catch (ShutdownSignalException | ConsumerCancelledException | IOException e) {
                    log.error("exception", e);

                    try {
                        channel.close();
                    } catch (IOException | TimeoutException ex) {
                        log.error("exception", ex);
                    }

                    channel = connection.createChannel(false);

                    return new DetailRes(false, "shutdown or cancelled exception " + e.toString());
                } catch (Exception e) {
                    log.info("exception : ", e);

                    try {
                        channel.close();
                    } catch (IOException | TimeoutException ex) {
                        ex.printStackTrace();
                    }

                    channel = connection.createChannel(false);

                    return new DetailRes(false, "exception " + e.toString());
                }
            }

            @Override
            public String dequeue() {
                GetResponse response = null;
                try {
                    //1 通过basicGet获取原始数据
                    response = channel.basicGet(queue, false);

//                    while (response == null) {
//                        response = channel.basicGet(queue, false);
//                        Thread.sleep(Constants.ONE_SECOND);
//                    }
                    if (response == null) {
                        return null;
                    }
                    Message message = new Message(response.getBody(),
                            messagePropertiesConverter.toMessageProperties(response.getProps(), response.getEnvelope(), "UTF-8"));

                    //2 将原始数据转换为特定类型的包
                    @SuppressWarnings("unchecked")
                    T messageBean = (T) messageConverter.fromMessage(message);

                    String result = messageProcess.process1(messageBean);
                    //4 手动发送ack确认
                    if (result != null && !"".equals(result)) {
                        channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
                    } else {
                        // 消费失败，消息重返队列队首 第二个参数是否应用于多消息，第三个参数是否requeue
                        //true则重新入队列，否则丢弃或者进入死信队列
                        channel.basicNack(response.getEnvelope().getDeliveryTag(), false, false);
                    }
                    return result;
                } catch (Exception e) {
                    log.error("exception", e);
                    // 消费失败，消息重返队列队首 第二个参数是否应用于多消息，第三个参数是否requeue
                    //true则重新入队列，否则丢弃或者进入死信队列
                    if (response != null) {
                        try {
                            channel.basicNack(response.getEnvelope().getDeliveryTag(), false, false);
                        } catch (IOException ex) {
                            log.error("nack exception:", ex);
                        }
                    }
                } finally {
                    try {
                        channel.close();
                    } catch (IOException | TimeoutException ex) {
                        log.error("exception", ex);
                    }
                }
                return null;
            }
        };
    }

    private void buildQueue(String exchange, String routingKey,
                            final String queue, Connection connection, String type) throws IOException {
        Channel channel = connection.createChannel(false);

        if (TYPE_DIRECT.equals(type)) {
            channel.exchangeDeclare(exchange, TYPE_DIRECT, true, false, null);
        } else if (TYPE_TOPIC.equals(type)) {
            channel.exchangeDeclare(exchange, TYPE_TOPIC, true, false, null);
        }

        channel.queueDeclare(queue, true, false, false, null);
        channel.queueBind(queue, exchange, routingKey);

        try {
            channel.close();
        } catch (TimeoutException e) {
            log.info("close channel time out ", e);
        }
    }

    private void buildTopic(String exchange, Connection connection) throws IOException {
        Channel channel = connection.createChannel(false);
        channel.exchangeDeclare(exchange, TYPE_TOPIC, true, false, null);
    }

    /**
     * for test
     */
    public int getMessageCount(final String queue) throws IOException {
        Connection connection = connectionFactory.createConnection();
        final Channel channel = connection.createChannel(false);
        final AMQP.Queue.DeclareOk declareOk = channel.queueDeclarePassive(queue);

        return declareOk.getMessageCount();
    }
}
