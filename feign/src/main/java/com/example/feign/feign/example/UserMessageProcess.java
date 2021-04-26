package com.example.feign.feign.example;


import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.rabbitmqinterface.MessageProcess;

/**
 * Created by littlersmall on 16/6/28.
 */
public class UserMessageProcess implements MessageProcess<UserMessage> {
    @Override
    public DetailRes process(UserMessage userMessage) {
        System.out.println(userMessage.getId() + userMessage.getName());

        return new DetailRes(true, "");
    }

    @Override
    public String process1(UserMessage message) {
        return message.getId() + "--" + message.getName();
    }


}
