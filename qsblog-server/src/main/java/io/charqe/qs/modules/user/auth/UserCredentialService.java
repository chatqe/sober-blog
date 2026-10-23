package io.charqe.qs.modules.user.auth;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/** 两个登录入口共用验密逻辑，凭据与账号状态始终从数据库读取。 */
@Service
public class UserCredentialService {
    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final AuthSessionService sessions;

    public UserCredentialService(UserMapper users, PasswordEncoder encoder, AuthSessionService sessions) {
        this.users = users;
        this.encoder = encoder;
        this.sessions = sessions;
    }

    public record Verified(User user, long revision) { }

    public Verified authenticate(String account, String encryptedPassword) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(encryptedPassword)) {
            throw new AuthAccessException(400, "请输入账号和密码");
        }
        String password;
        try {
            password = new String(SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8))
                    .decrypt(encryptedPassword), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            throw new AuthAccessException(400, "密码格式错误");
        }
        User found = users.selectOne(Wrappers.<User>lambdaQuery().and(q -> q
                .eq(User::getUsername, account.trim()).or().eq(User::getEmail, account.trim())
                .or().eq(User::getPhoneNumber, account.trim())));
        if (found == null) throw invalid();
        long revision = sessions.revision(found.getId());
        // 版本读取之后重新取凭据，不能用安全事件之前查到的密码快照。
        User user = users.selectById(found.getId());
        AuthSessionService.checkUser(user);
        String hash = user.getPassword();
        if (!matches(password, hash)) throw invalid();
        if (hash.matches("[a-fA-F0-9]{32}")) {
            String upgraded = encode(password);
            int updated = users.update(null, Wrappers.<User>lambdaUpdate()
                    .eq(User::getId, user.getId()).eq(User::getPassword, hash)
                    .set(User::getPassword, upgraded));
            if (updated != 1) throw invalid();
            user.setPassword(upgraded);
        }
        return new Verified(user, revision);
    }

    public boolean matches(String password, String hash) {
        if (!StringUtils.hasText(password) || !StringUtils.hasText(hash)) return false;
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            return encoder.matches(password, hash);
        }
        return hash.matches("[a-fA-F0-9]{32}")
                && DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8)).equalsIgnoreCase(hash);
    }

    public String encode(String password) {
        if (!StringUtils.hasText(password)) throw new AuthAccessException(400, "密码不能为空");
        return encoder.encode(password);
    }

    private static AuthAccessException invalid() {
        return new AuthAccessException(401, "账号或密码错误");
    }
}
