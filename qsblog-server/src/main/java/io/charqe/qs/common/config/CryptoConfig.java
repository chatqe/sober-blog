package io.charqe.qs.common.config;

import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.constants.InfraConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 从配置文件注入加密密钥到常量类
 *
 * @author Joy
 * @since 2026-06-11
 */
@Slf4j
@Configuration
public class CryptoConfig {

    @Value("${crypto.aes-key}")
    private String aesKey;

    @PostConstruct
    public void init() {
        CommonConst.CRYPOTJS_KEY = aesKey;
        InfraConst.CRYPOTJS_KEY = aesKey;
        log.info("AES加密密钥已从配置文件加载");
    }
}
