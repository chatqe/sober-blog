package io.charqe.qs.modules.user.auth;

import io.charqe.qs.modules.user.auth.SaTokenSessionAdapter.Metadata;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 纯单元测试：不启动应用，不接触业务数据库或 Redis。 */
class AuthSessionServiceTest {
    SaTokenSessionAdapter adapter;
    UserMapper users;
    ApplicationEventPublisher events;
    AuthSessionService sessions;
    final AuthSessionProperties properties = new AuthSessionProperties();
    final long now = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();

    @BeforeEach
    void setup() {
        adapter = mock(SaTokenSessionAdapter.class);
        users = mock(UserMapper.class);
        events = mock(ApplicationEventPublisher.class);
        sessions = new AuthSessionService(adapter, properties, users,
                Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC), events);
    }

    @AfterEach
    void cleanup() { RequestContextHolder.resetRequestAttributes(); }

    static User user(long id, int role) {
        User user = new User();
        user.setId(id);
        user.setUserType(role);
        user.setUserStatus(1);
        user.setUsername("测试用户");
        user.setPassword("不应出现在响应中");
        return user;
    }

    Metadata existing(String token, long id, SessionScope scope, int role) {
        Metadata m = new Metadata();
        m.setSessionId(token);
        m.setUserId(id);
        m.setSessionType(scope);
        m.setIssuedAt(now);
        m.setIdleExpiresAt(now + properties.policy(scope).getIdle().toMillis());
        m.setAbsoluteExpiresAt(now + properties.policy(scope).getAbsolute().toMillis());
        when(adapter.find(token)).thenReturn(m);
        when(adapter.resolve(eq(token), same(m), anyLong(), anyBoolean(), anyLong())).thenReturn(m);
        when(users.selectById(id)).thenReturn(user(id, role));
        return m;
    }

    @ParameterizedTest
    @CsvSource({"0,WEB,true", "1,WEB,true", "2,WEB,true", "0,ADMIN,true", "1,ADMIN,true", "2,ADMIN,false"})
    void loginMatrix(int role, SessionScope scope, boolean allowed) {
        if (!allowed) {
            assertEquals(403, assertThrows(AuthAccessException.class,
                    () -> sessions.issue(user(1, role), scope, 0)).getStatus());
            verifyNoInteractions(adapter);
            return;
        }
        when(adapter.issue(any())).thenReturn("new-token");
        AuthSessionVO result = sessions.issue(user(1, role), scope, 0);
        assertEquals(scope, result.getSessionType());
        assertEquals("new-token", result.getAccessToken());
        assertEquals(now + properties.policy(scope).getAbsolute().toMillis(), result.getAbsoluteExpiresAt());
        String json = com.alibaba.fastjson2.JSON.toJSONString(result);
        assertFalse(json.contains("password"));
        assertFalse(json.contains("openId"));
    }

    @Test
    void rejectsUnknownRoleAndNonIssuableScope() {
        assertThrows(AuthAccessException.class, () -> sessions.issue(user(1, -1), SessionScope.WEB, 0));
        assertThrows(IllegalArgumentException.class, () -> sessions.issue(user(1, 0), SessionScope.EITHER, 0));
        assertThrows(IllegalArgumentException.class, () -> sessions.issue(user(1, 0), null, 0));
        verifyNoInteractions(adapter);
    }

    @Test
    void noCrossDomainAuthenticationOrRenewal() {
        existing("web", 1, SessionScope.WEB, 0);
        existing("admin", 2, SessionScope.ADMIN, 1);
        assertEquals(1L, sessions.resolve("Bearer web", SessionScope.WEB, true).user().getId());
        assertEquals(2L, sessions.resolve("admin", SessionScope.ADMIN, false).user().getId());
        assertEquals(403, assertThrows(AuthAccessException.class,
                () -> sessions.resolve("web", SessionScope.ADMIN, true)).getStatus());
        verify(adapter, never()).resolve(eq("admin"), any(), anyLong(), eq(true), anyLong());
    }

    @Test
    void passiveInspectionDoesNotTouchAndCachesDatabaseLookup() {
        Metadata metadata = existing("admin", 2, SessionScope.ADMIN, 0);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "admin");
        request.addHeader("X-Session-Activity", "passive");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        assertNull(sessions.inspect(SessionScope.ADMIN).getAccessToken());
        sessions.current(SessionScope.ADMIN, true);
        sessions.currentUserOrNull();
        verify(users, times(1)).selectById(2L);
        verify(adapter, never()).resolve(any(), any(), anyLong(), eq(true), anyLong());
        assertEquals(now + Duration.ofHours(2).toMillis(), metadata.getIdleExpiresAt());
    }

    @Test
    void logoutOnlyRevokesSelectedSessionAndIsIdempotent() {
        existing("web", 1, SessionScope.WEB, 0);
        existing("admin", 1, SessionScope.ADMIN, 0);
        assertEquals(403, assertThrows(AuthAccessException.class,
                () -> sessions.logout("admin", SessionScope.WEB)).getStatus());
        sessions.logout("web", SessionScope.WEB);
        sessions.logout("missing", SessionScope.WEB);
        verify(adapter).delete("web", SessionScope.WEB);
        verify(adapter, never()).delete("admin", SessionScope.ADMIN);
        verify(events).publishEvent(new AuthSessionService.Revoked(1L, "web", SessionScope.WEB));
    }

    @Test
    void rejectsDeletedOrRevokedSessionsAndDisabledUsers() {
        Metadata m = existing("admin", 2, SessionScope.ADMIN, 0);
        when(adapter.resolve(eq("admin"), same(m), anyLong(), eq(true), anyLong())).thenReturn(null);
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> sessions.resolve("admin", SessionScope.ADMIN, true)).getStatus());
        User disabled = user(2, 0);
        disabled.setUserStatus(0);
        when(users.selectById(2L)).thenReturn(disabled);
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> sessions.resolve("admin", SessionScope.ADMIN, false)).getStatus());
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> sessions.resolve("USER_old-token", SessionScope.WEB, false)).getStatus());
    }

    @Test
    void safetyEventRejectsConcurrentOldCredentialIssuance() {
        when(adapter.issue(any())).thenReturn(null);
        assertEquals(401, assertThrows(AuthAccessException.class,
                () -> sessions.issue(user(1, 0), SessionScope.WEB, 3)).getStatus());
        sessions.revokeAll(1L);
        verify(adapter).revokeAll(1L);
        verify(events).publishEvent(new AuthSessionService.Revoked(1L, null, SessionScope.EITHER));
    }

    @Test
    void durationsMustMatchFrameworkWholeSecondPrecision() {
        properties.validate();
        properties.getAdmin().setIdle(Duration.ofMillis(500));
        assertThrows(IllegalArgumentException.class, properties::validate);
        properties.getAdmin().setIdle(Duration.ofHours(13));
        assertThrows(IllegalArgumentException.class, properties::validate);
    }
}
