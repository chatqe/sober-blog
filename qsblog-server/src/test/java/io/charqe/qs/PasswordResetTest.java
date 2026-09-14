package io.charqe.qs;

import io.charqe.qs.utils.AESUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;


/**
 * 密码重置工具测试类
 * 生成目标密码的密文，直接 UPDATE 数据库后生效
 */
@SpringBootTest(classes = QsBlogApplication.class)
@ActiveProfiles("dev")
@Slf4j
public class PasswordResetTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 打印指定明文密码的各种密文格式
     */
    @Test
    void printPasswordHashes() {
        String newPwd = "123456"; // ← 改成你想设的密码

        // BCrypt（推荐，与 UserServiceImpl.encodePassword 一致）
        String bCrypt = passwordEncoder.encode(newPwd);
        // MD5（兼容旧格式，与 AESUtil.md5Hex 一致）
        String md5 = AESUtil.md5Hex(newPwd);

        log.info("=======================================");
        log.info("明文密码: {}", newPwd);
        log.info("---------------------------------------");
        log.info("BCrypt密文 (推荐):");
        log.info("  {}", bCrypt);
        log.info("---------------------------------------");
        log.info("MD5密文 (旧格式):");
        log.info("  {}", md5);
        log.info("=======================================");

    }
}
