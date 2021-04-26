package com.example.feign.feign.rabbitmqinterface;

import com.example.feign.feign.common.DetailRes;

/**
 * Created by littlersmall on 16/5/12.
 */
public interface MessageConsumer {
    DetailRes consume();

    String dequeue();
}
