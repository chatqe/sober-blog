package io.charqe.qs.utils;

import cn.hutool.crypto.SecureUtil;
import io.charqe.qs.constants.CommonConst;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * aes工具类
 *
 * @author sjq
 * @since 2025-12-10 21:23
 */
public class AESUtil {

    /**
     * 加密
     *
     * @param pwd 明文
     * @return
     */
    public static String encrypt(String pwd) {
        return DigestUtils.md5DigestAsHex(pwd.getBytes());
    }

    /**
     * 解密
     *
     * @param decPwd 密文
     * @return
     */
    public static String decrypt(String decPwd) {
        return new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).decrypt(decPwd));
    }


}


