package io.charqe.qs.modules.user.auth;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserCredentialServiceTest {
    UserMapper users;
    AuthSessionService sessions;
    UserCredentialService credentials;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    String previousKey;

    @BeforeEach
    void setup() {
        previousKey = CommonConst.CRYPOTJS_KEY;
        CommonConst.CRYPOTJS_KEY = "0123456789abcdef";
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "auth-unit-test"), User.class);
        users = mock(UserMapper.class);
        sessions = mock(AuthSessionService.class);
        credentials = new UserCredentialService(users, encoder, sessions);
    }

    @AfterEach
    void cleanup() { CommonConst.CRYPOTJS_KEY = previousKey; }

    String encrypt(String password) {
        return SecureUtil.aes(CommonConst.CRYPOTJS_KEY.getBytes(StandardCharsets.UTF_8)).encryptBase64(password);
    }

    User stored(String hash) {
        User user = AuthSessionServiceTest.user(7, 2);
        user.setPassword(hash);
        when(users.selectOne(any())).thenReturn(user);
        when(users.selectById(7L)).thenReturn(user);
        return user;
    }

    @Test
    void rejectsEmptyHashAndMalformedPassword() {
        stored("");
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> credentials.authenticate("tester", encrypt("secret"))).getStatus());
        assertFalse(credentials.matches("secret", null));
        assertFalse(credentials.matches("", encoder.encode("")));
        assertEquals(400, assertThrows(AuthAccessException.class,
                () -> credentials.authenticate("tester", "bad-encrypted-input")).getStatus());
    }

    @Test
    void bcryptReadsRevisionBeforeFreshDatabaseCredentials() {
        stored(encoder.encode("secret"));
        when(sessions.revision(7L)).thenReturn(9L);
        assertEquals(9L, credentials.authenticate("tester", encrypt("secret")).revision());
        var order = inOrder(users, sessions);
        order.verify(users).selectOne(any());
        order.verify(sessions).revision(7L);
        order.verify(users).selectById(7L);
        verify(users, never()).update(any(), any());
    }

    @Test
    void oldSnapshotCannotBeUsedWithNewRevision() {
        User old = stored(encoder.encode("old"));
        User current = AuthSessionServiceTest.user(7, 2);
        current.setPassword(encoder.encode("new"));
        when(users.selectById(7L)).thenReturn(current);
        when(sessions.revision(7L)).thenReturn(2L);
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> credentials.authenticate(old.getUsername(), encrypt("old"))).getStatus());
    }

    @Test
    void upgradesMd5WithConditionalUpdate() {
        String hash = DigestUtils.md5DigestAsHex("secret".getBytes(StandardCharsets.UTF_8));
        User user = stored(hash);
        when(users.update(isNull(), any())).thenReturn(1);
        credentials.authenticate("tester", encrypt("secret"));
        assertTrue(encoder.matches("secret", user.getPassword()));
        verify(users).update(isNull(), argThat(wrapper -> wrapper.getExpression().getNormal().size() > 0));
    }

    @Test
    void concurrentPasswordChangeAbortsMd5Upgrade() {
        stored(DigestUtils.md5DigestAsHex("secret".getBytes(StandardCharsets.UTF_8)));
        when(users.update(isNull(), any())).thenReturn(0);
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> credentials.authenticate("tester", encrypt("secret"))).getStatus());
    }
}
