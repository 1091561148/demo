package com.example.client.client.Controller;

import com.example.client.client.DTO.UserDTO;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/client")
public class ClientController {
    private final Logger logger = Logger.getLogger("ClientController.class");
    @Resource
    private DiscoveryClient client;


    @RequestMapping(value = "/hello", method = RequestMethod.GET)
    public UserDTO index(String inputName) {
        String description = client.description();
        List<String> services = client.getServices();
        for (String service : services) {
            List<ServiceInstance> instances = client.getInstances(service);

            //String instancesStr = JSONUtil.toJsonPrettyStr(instances);
            //System.out.println("instancesStr== " + instancesStr);
        }
        logger.info("hello :" + inputName);
        UserDTO userDTO = new UserDTO();
        userDTO.setId("13");
        userDTO.setName("name");
        return userDTO;
    }

}
