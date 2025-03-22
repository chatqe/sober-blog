package com.ld.poetry.utils;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

/**
 * token生成工具类
 *
 * @author sjq
 * @since 2024-09-14 10:10
 */
@Slf4j
public class JWTUtils {

    private static final String BASE64_SECRET_KEY = "RMNckTHUvjXE1xrIMirOKWSaZKzHvlYCThEXxKXK8AU=a";
    private static final Integer EXPIRATION_TIME = 1000 * 60 * 2 * 60;
    private static final String SECRET_KEY = "huo!!!jiuzhegeya";

    /**
     * 生成随机32位字节的密匙字符串
     * RMNckTHUvjXE1xrIMirOKWSaZKzHvlYCThEXxKXK8AU=
     *
     * @param args
     */
    public static void main(String[] args) {
        // 创建一个长度为32的字节数组
        byte[] bytes = new byte[32];
        // 创建了一个SecureRandom类的实例,SecureRandom类提供了密码学强度的随机数生成器
        SecureRandom secureRandom = new SecureRandom();
        // 调用了SecureRandom对象的nextBytes方法，它会用随机数填充之前创建的字节数组。这意味着数组中的每个元素都将被一个随机的字节值替换。
        secureRandom.nextBytes(bytes);

        // 32字节的随机数据转化为 字符串
        String secretKey = Base64.getEncoder().encodeToString(bytes);
        log.info("secretKey: {}", secretKey);
    }

    public static String createToken() {
        // 默认使用sha256加密
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        // 当前时间
        long currentTimeMillis = System.currentTimeMillis();
        Date now = new Date(currentTimeMillis);

        JwtBuilder jwtBuilder = Jwts.builder()
                // header 头
                .setHeaderParam("typ", "JWT")
                // payload 载荷
                // TODO 待完善
                .setClaims(new HashMap<>())
                // signature 签名
                .signWith(signatureAlgorithm, BASE64_SECRET_KEY);

        // 过期时间
        if (EXPIRATION_TIME > 0) {
            // 到期时间
            long expMillis = currentTimeMillis + EXPIRATION_TIME;
            Date exp = new Date(expMillis);
            jwtBuilder.setExpiration(exp).setNotBefore(now);
        }

        // 生成token
        String compact = jwtBuilder.compact();
        log.info("生成jwt令牌=========={}", compact);
        return compact;

    }

}
