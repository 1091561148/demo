package com.example.feign.feign.Interface;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("mytestclient")
public interface HelloService {
    @GetMapping("/client/hello")
    public String getConsumer(@RequestParam("inputName") String inputName);
}
