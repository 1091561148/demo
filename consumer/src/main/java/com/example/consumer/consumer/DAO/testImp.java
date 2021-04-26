package com.example.consumer.consumer.DAO;


import com.test.shiro.constant.LoginService;
import com.test.shiro.entity.UserEntity;
import org.apache.shiro.crypto.hash.SimpleHash;
import org.apache.shiro.util.ByteSource;
import org.springframework.stereotype.Service;


@Service
public class testImp implements LoginService {
    @Override
    public UserEntity getSystemUser(String userName, String password) {
        System.out.println("run errr_:" + userName + "--:" + password);
        UserEntity user = new UserEntity();
        if (("123").equals(userName)) {
            user.setOrgName("123");
            user.setLoginName("qwe");
            user.setLoginPasswd(md5("teuedsafd", "qwe"));
            return user;
        } else {
            return null;
        }
    }

    public String md5(String password, String salt) {
        //加密方式
        String hashAlgorithmName = "md5";
        //盐：为了即使相同的密码不同的盐加密后的结果也不同
        ByteSource byteSalt = ByteSource.Util.bytes(salt);
        //密码
        Object source = password;
        //加密次数
        int hashIterations = 1024;
        SimpleHash result = new SimpleHash(hashAlgorithmName, source, byteSalt, hashIterations);
        return result.toString();
    }
}
