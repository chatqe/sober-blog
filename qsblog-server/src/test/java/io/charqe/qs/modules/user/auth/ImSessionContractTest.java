package io.charqe.qs.modules.user.auth;

import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.im.websocket.ImWsMsgHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.tio.core.ChannelContext;
import org.tio.core.TioConfig;
import org.tio.core.task.CloseRunnable;
import org.tio.core.task.DecodeRunnable;
import org.tio.core.task.HandlerRunnable;
import org.tio.core.task.SendRunnable;
import org.tio.http.common.HttpRequest;
import org.tio.http.common.HttpResponse;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证 IM 对统一会话的消费边界；通道关闭队列为 mock，不建立网络连接或加载消息数据库。 */
class ImSessionContractTest {
    private AuthSessionService sessions;
    private ImWsMsgHandler handler;

    @BeforeEach
    void setup() {
        sessions = mock(AuthSessionService.class);
        handler = new ImWsMsgHandler();
        ReflectionTestUtils.setField(handler, "sessions", sessions);
    }

    private ChannelContext channel() {
        var channel = mock(ChannelContext.class);
        channel.decodeRunnable = mock(DecodeRunnable.class);
        channel.handlerRunnable = mock(HandlerRunnable.class);
        channel.sendRunnable = mock(SendRunnable.class);
        channel.closeMeta = mock(ChannelContext.CloseMeta.class);
        ReflectionTestUtils.setField(channel, "closeLock", new ReentrantReadWriteLock());
        channel.tioConfig = mock(TioConfig.class);
        channel.tioConfig.closeRunnable = mock(CloseRunnable.class);
        return channel;
    }

    private AuthSessionService.Resolved identity(String token, long userId, String sessionId) {
        var metadata = new SaTokenSessionAdapter.Metadata();
        metadata.setUserId(userId);
        metadata.setSessionType(SessionScope.WEB);
        metadata.setSessionId(sessionId);
        return new AuthSessionService.Resolved(metadata, AuthSessionServiceTest.user(userId, 0), token);
    }

    private HttpResponse handshake(ChannelContext channel, String token) {
        var request = mock(HttpRequest.class);
        var response = mock(HttpResponse.class);
        when(request.getParam(CommonConst.TOKEN_HEADER)).thenReturn(token);
        return handler.handshake(request, response, channel);
    }

    private ChannelContext connected(String token, long userId, String sessionId) {
        var channel = channel();
        when(sessions.resolve(token, SessionScope.WEB, false)).thenReturn(identity(token, userId, sessionId));
        assertNotNull(handshake(channel, token));
        return channel;
    }

    @Test
    void handshakeRejectsMissingLegacyOrAdminCredentials() {
        var channel = channel();
        assertNull(handshake(channel, null));
        verifyNoInteractions(sessions);
        when(sessions.resolve("legacy", SessionScope.WEB, false)).thenThrow(new AuthAccessException(401, "旧会话"));
        when(sessions.resolve("admin", SessionScope.WEB, false)).thenThrow(new AuthAccessException(403, "会话域不符"));
        assertNull(handshake(channel, "legacy"));
        assertNull(handshake(channel, "admin"));
        assertFalse(handler.checkConnection(channel, true));
        verify(sessions, never()).resolve(anyString(), any(), eq(true));
    }

    @Test
    void handshakeAndScheduledChecksArePassive() {
        var channel = connected("web", 1, "web-session");
        handler.checkIdleConnections();
        assertTrue(handler.checkConnection(channel, false));
        verify(sessions, times(3)).resolve("web", SessionScope.WEB, false);
        verify(sessions, never()).resolve(anyString(), any(), eq(true));
        verifyNoInteractions(channel.tioConfig.closeRunnable);
    }

    @Test
    void adminLogoutNeverClosesWebConnections() {
        var channel = connected("web", 1, "web-session");
        clearInvocations(sessions);
        handler.onSessionRevoked(new AuthSessionService.Revoked(1L, "web-session", SessionScope.ADMIN));
        verifyNoInteractions(sessions, channel.tioConfig.closeRunnable);
        assertTrue(handler.checkConnection(channel, false));
    }

    @Test
    void webLogoutClosesOnlyMatchingSessionNotOtherDevicesOrUsers() {
        var selected = connected("selected", 1, "selected-session");
        var otherDevice = connected("device", 1, "device-session");
        var otherUser = connected("other", 2, "other-session");
        handler.onSessionRevoked(new AuthSessionService.Revoked(1L, "selected-session", SessionScope.WEB));
        verify(selected.closeMeta).setNeedRemove(true);
        verify(selected.tioConfig.closeRunnable).execute();
        assertFalse(handler.checkConnection(selected, false));
        assertTrue(handler.checkConnection(otherDevice, false));
        assertTrue(handler.checkConnection(otherUser, false));
        verifyNoInteractions(otherDevice.tioConfig.closeRunnable, otherUser.tioConfig.closeRunnable);
    }

    @Test
    void safetyEventRevalidatesVersionAndPreservesPostEventLogin() {
        var old = connected("old", 1, "old-session");
        var newer = connected("new", 1, "new-session");
        var otherUser = connected("other", 2, "other-session");
        when(sessions.resolve("old", SessionScope.WEB, false)).thenThrow(new AuthAccessException(401, "版本已撤销"));
        clearInvocations(sessions);
        handler.onSessionRevoked(new AuthSessionService.Revoked(1L, null, SessionScope.EITHER));
        verify(old.tioConfig.closeRunnable).execute();
        verify(sessions).resolve("new", SessionScope.WEB, false);
        verify(sessions, never()).resolve(eq("other"), any(), anyBoolean());
        assertFalse(handler.checkConnection(old, false));
        assertTrue(handler.checkConnection(newer, false));
        verifyNoInteractions(newer.tioConfig.closeRunnable, otherUser.tioConfig.closeRunnable);
    }

    @Test
    void idleExpiryClosesConnectionWithoutTouchingIt() {
        var channel = connected("web", 1, "web-session");
        when(sessions.resolve("web", SessionScope.WEB, false)).thenThrow(new AuthAccessException(401, "闲置过期"));
        handler.checkIdleConnections();
        verify(channel.tioConfig.closeRunnable).execute();
        assertFalse(handler.checkConnection(channel, true));
        verify(sessions, never()).resolve(anyString(), any(), eq(true));
    }

    @Test
    void emptyOrUnsupportedMessagesDoNotRenew() {
        var channel = connected("web", 1, "web-session");
        handler.onText(null, " ", channel);
        handler.onText(null, "{\"content\":\"\",\"messageType\":1}", channel);
        handler.onText(null, "{\"content\":\"测试\",\"messageType\":-1}", channel);
        verify(sessions, never()).resolve(anyString(), any(), eq(true));
    }

    @Test
    void forgottenChannelIsNotRevalidatedOrRenewed() {
        var channel = connected("web", 1, "web-session");
        handler.forget(channel);
        clearInvocations(sessions);
        assertFalse(handler.checkConnection(channel, true));
        handler.checkIdleConnections();
        verifyNoInteractions(sessions);
    }
}
