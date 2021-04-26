package com.example.feign.feign.Controller;

import com.example.feign.feign.Interface.HelloService;
import com.example.feign.feign.common.DetailRes;
import com.example.feign.feign.example.ConsumerExample;
import com.example.feign.feign.example.SenderExample;
import com.example.feign.feign.example.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
@RequestMapping("/feign")
public class MainController {

    @Autowired
    private HelloService helloService;

    @Autowired
    private SenderExample senderExample;

    @Autowired
    private ConsumerExample consumerExample;

    @RequestMapping(value = "/runerror", method = RequestMethod.GET)
    public String getConsumer() {
        return helloService.getConsumer("ren shi wo sa de");
    }

    @RequestMapping(value = "/enqueueMessage", method = RequestMethod.GET)
    public String enqumessage(String message) {
        DetailRes res = senderExample.send(new UserMessage(new Random(100).nextInt(), message));
        return res.getErrMsg();
    }

    @RequestMapping(value = "/dequeueMessage", method = RequestMethod.GET)
    public String dequeue() {
        return consumerExample.dequeue();
    }
}
