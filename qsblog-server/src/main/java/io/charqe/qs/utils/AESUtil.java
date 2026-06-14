package io.charqe.qs.utils;

import cn.hutool.crypto.SecureUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * AES工具类（加密/解密 + MD5哈希）
 *
 * @author sjq
 * @since 2025-12-10 21:23
 */
@Component
public class AESUtil {

    private static String aesKey;

    @Value("${crypto.aes-key}")
    public void setAesKey(String key) {
        AESUtil.aesKey = key;
    }

    /**
     * MD5哈希（注意：这是哈希，不是AES加密）
     *
     * @param pwd 明文
     * @return MD5 hex字符串
     */
    public static String md5Hex(String pwd) {
        return DigestUtils.md5DigestAsHex(pwd.getBytes());
    }

    /**
     * 加密（已废弃，请使用 {@link #md5Hex}）
     *
     * @param pwd 明文
     * @return MD5 hex字符串
     * @deprecated 方法名误导（实际执行MD5哈希而非AES加密），请使用 {@link #md5Hex(String)}
     */
    @Deprecated
    public static String encrypt(String pwd) {
        return md5Hex(pwd);
    }

    /**
     * AES解密
     *
     * @param decPwd 密文
     * @return 明文
     */
    public static String decrypt(String decPwd) {
        return new String(SecureUtil.aes(aesKey.getBytes(StandardCharsets.UTF_8)).decrypt(decPwd));
    }

}


