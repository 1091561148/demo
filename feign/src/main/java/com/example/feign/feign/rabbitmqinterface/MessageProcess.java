package com.example.feign.feign.rabbitmqinterface;


import com.example.feign.feign.common.DetailRes;

/**
 * Created by littlersmall on 16/5/11.
 */
public interface MessageProcess<T> {
    DetailRes process(T message);

    String process1(T message);
}
