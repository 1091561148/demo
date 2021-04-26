package com.example.feign.feign.rabbitmqinterface;

import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.common.MessageWithTime;

/**
 * Created by littlersmall on 16/5/12.
 */
public interface MessageSender {
    DetailRes send(Object message);

    DetailRes send(MessageWithTime messageWithTime);
}
