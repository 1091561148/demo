package com.example.consumer.consumer.Controller;

import com.example.consumer.consumer.DTO.UserDTO;
import com.test.shiro.entity.UserEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
public class MainController {

    @Autowired
    RestTemplate restTemplate;

    @RequestMapping(value = "/ribbon-consumer", method = RequestMethod.GET)
    public UserDTO getConsumer() {
        return restTemplate.getForEntity("http://MYTESTCLIENT/client/hello?inputName={1}", UserDTO.class, "test").getBody();
    }


    @RequestMapping(value = "/login", method = RequestMethod.GET)
    @ResponseBody
    public String login(String username, String password, String ifRemember) {

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return "用户名密码不能为空";
        }

        boolean rememberMe = false;
        if ("on".equals(ifRemember)) {
            rememberMe = true;
        }

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password, rememberMe);
        try {
            subject.login(token);
//            Session session = subject.getSession(false);
//            String url = WebUtils.getSavedRequest(request).getRequestUrl();
            UserEntity user = (UserEntity) SecurityUtils.getSubject().getPrincipal();

            return "success:" + user.getLoginName() + user.getOrgName();
        } catch (LockedAccountException lae) {
            token.clear();
            return "用户已经被锁定不能登录，请与管理员联系！";
        } catch (AuthenticationException e) {
            System.out.println("error:" + e.getMessage());
            token.clear();
            return "用户名或密码错误！";
        }

    }
}
