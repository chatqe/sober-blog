package io.charqe.qs.utils;

import io.charqe.qs.modules.user.controller.web.dto.LoginUserInfo;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
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
    /**
     * 默认过期时间 2小时
     */
    private static final Integer EXPIRATION_TIME = 1000 * 60 * 2 * 60;
    private static final String SECRET_KEY = "huo!!!jiuzhegeya";

    // 令牌前缀
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USER_NAME = "username";
    public static final String CLAIM_USER_TYPE = "userType";
    public static final String CLAIM_AVATAR = "avatar";
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

    public  String createToken(LoginUserInfo userInfo) {

        // 构建加密密钥（使用HS256算法，密钥长度必须>=256位）
        SecretKey key = Keys.hmacShaKeyFor(BASE64_SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        // 过期时间
        Date expireDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME);

        // 封装用户信息
        HashMap<String, Object> claims = new HashMap<>(8);
        claims.put(CLAIM_USER_ID, userInfo.getId());
        claims.put(CLAIM_USER_NAME, userInfo.getUsername());
        claims.put(CLAIM_USER_TYPE, userInfo.getUserType());
        claims.put(CLAIM_AVATAR, userInfo.getAvatar());
        // claims.put("roles", roles);

        JwtBuilder jwtBuilder = Jwts.builder()
                // header 头
                .setHeaderParam("typ", "JWT")
                // payload 载荷
                .setClaims(claims)
                // signature 签名
                .signWith(key,SignatureAlgorithm.HS256)
                // expireDate 过期时间
                .setExpiration(expireDate);

        // 压缩编码 生成token
        String compact = jwtBuilder.compact();
        log.info("生成jwt令牌=========={}", compact);
        return compact;

    }

    /**
     * 解析Token，获取所有载荷信息
     * @param token JWT Token
     * @return 载荷Claims对象
     * @throws JwtException Token无效/过期/签名错误等异常
     */
    public Claims parseToken(String token) throws JwtException {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }
        // 移除Token前缀（如存在）
        if (token.startsWith(TOKEN_PREFIX)) {
            token = token.substring(TOKEN_PREFIX.length());
        }

        SecretKey key = Keys.hmacShaKeyFor(BASE64_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        // 解析Token并验证签名和过期时间
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从Token中获取用户ID
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_USER_ID, Long.class);
    }

    /**
     * 从Token中获取用户昵称
     * @param token JWT Token
     * @return 用户昵称
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_USER_NAME, String.class);
    }

    /**
     * 从Token中获取用户头像
     * @param token JWT Token
     * @return 用户头像URL
     */
    public String getAvatar(String token) {
        Claims claims = parseToken(token);
        return claims.get(CLAIM_AVATAR, String.class);
    }

    /**
     * 验证Token是否有效（未过期+签名正确）
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("Token已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token格式不支持: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.error("Token签名错误: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Token参数错误: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("Token验证失败: {}", e.getMessage());
        }
        return false;
    }


    /**
     * 获取token中的信息 并转化为userInfo
     *
     * @param token
     * @return
     */
    public  LoginUserInfo parseUser(String token) {
        Claims claims = parseToken(token);
        return new LoginUserInfo(
                claims.get(CLAIM_USER_ID, Long.class),
                claims.get(CLAIM_USER_NAME, String.class),
                claims.get(CLAIM_USER_TYPE, Integer.class),
                claims.get(CLAIM_AVATAR, String.class)
        );
    }

}


