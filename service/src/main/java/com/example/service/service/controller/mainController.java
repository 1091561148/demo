package com.example.service.service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class mainController {

    @RequestMapping("hello")
    public String hello() {

        return "hello";
    }

}
