package com.example.service.service.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "personinfo")
@Data
public class PersonInfo {

    public String name;
    public String code;
}
