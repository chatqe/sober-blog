package io.charqe.qs.modules.user.auth;

import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.auth.SaTokenSessionAdapter.Metadata;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthSessionService {
    private static final String ATTRIBUTE = AuthSessionService.class.getName();
    private final SaTokenSessionAdapter adapter;
    private final AuthSessionProperties properties;
    private final UserMapper users;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public AuthSessionService(SaTokenSessionAdapter adapter, AuthSessionProperties properties,
                              UserMapper users, Clock clock, ApplicationEventPublisher events) {
        this.adapter = adapter;
        this.properties = properties;
        this.users = users;
        this.clock = clock;
        this.events = events;
    }

    public record Resolved(Metadata session, User user, String token) { }
    public record Revoked(Long userId, String sessionId, SessionScope scope) { }

    public long revision(Long userId) { return adapter.revision(userId); }

    public AuthSessionVO issue(User user, SessionScope scope, long revision) {
        properties.policy(scope);
        checkUser(user);
        if (scope == SessionScope.ADMIN && !isManager(user.getUserType())) {
            throw new AuthAccessException(403, "该账号没有后台访问权限");
        }
        long now = clock.millis();
        AuthSessionProperties.Policy policy = properties.policy(scope);
        Metadata session = new Metadata();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(user.getId());
        session.setSessionType(scope);
        session.setIssuedAt(now);
        session.setIdleExpiresAt(now + policy.getIdle().toMillis());
        session.setAbsoluteExpiresAt(now + policy.getAbsolute().toMillis());
        session.setRevision(revision);
        String token = adapter.issue(session);
        if (token == null) {
            throw new AuthAccessException(401, "账号安全状态已变更，请重新登录");
        }
        return AuthSessionVO.from(session, user, token);
    }

    public Resolved resolve(String token, SessionScope scope, boolean touch) {
        Metadata snapshot = adapter.find(normalize(token));
        if (snapshot == null || snapshot.getSessionType() == null) throw expired();
        Metadata session = adapter.resolve(normalize(token), snapshot, clock.millis(), false,
                properties.policy(snapshot.getSessionType()).getIdle().toMillis());
        if (session == null) throw expired();
        if (!scope.accepts(session.getSessionType())) {
            throw new AuthAccessException(403, "当前会话不能用于此入口");
        }
        User user = users.selectById(session.getUserId());
        checkUser(user);
        if (session.getSessionType() == SessionScope.ADMIN && !isManager(user.getUserType())) {
            throw new AuthAccessException(401, "后台权限已变更，请重新登录");
        }
        Resolved resolved = new Resolved(session, user, normalize(token));
        return touch ? touch(resolved) : resolved;
    }

    public Resolved current(SessionScope scope, boolean touch) {
        HttpServletRequest request = request();
        String token = request == null ? null : normalize(request.getHeader("Authorization"));
        Resolved resolved = request == null ? null : (Resolved) request.getAttribute(ATTRIBUTE);
        if (resolved == null || !Objects.equals(resolved.token(), token)) {
            resolved = resolve(token, scope, false);
            if (request != null) request.setAttribute(ATTRIBUTE, resolved);
        }
        if (!scope.accepts(resolved.session().getSessionType())) {
            throw new AuthAccessException(403, "当前会话不能用于此入口");
        }
        // 自动轮询可以显式声明不续期；客户端不能延长绝对寿命。
        boolean active = touch && (request == null || !"passive".equals(request.getHeader("X-Session-Activity")));
        if (active) {
            resolved = touch(resolved);
            if (request != null) request.setAttribute(ATTRIBUTE, resolved);
        }
        return resolved;
    }

    private Resolved touch(Resolved resolved) {
        Metadata session = adapter.resolve(resolved.token(), resolved.session(), clock.millis(), true,
                properties.policy(resolved.session().getSessionType()).getIdle().toMillis());
        if (session == null) throw expired();
        return new Resolved(session, resolved.user(), resolved.token());
    }

    public User currentUserOrNull() {
        try { return current(SessionScope.EITHER, false).user(); }
        catch (AuthAccessException e) { return null; }
    }

    public AuthSessionVO inspect(SessionScope scope) {
        Resolved r = current(scope, false);
        return AuthSessionVO.from(r.session(), r.user(), null);
    }

    public void logout(String token, SessionScope expected) {
        Metadata session = adapter.find(normalize(token));
        if (session == null) return;
        if (!expected.accepts(session.getSessionType())) {
            throw new AuthAccessException(403, "请从对应入口退出登录");
        }
        adapter.delete(normalize(token), session.getSessionType());
        events.publishEvent(new Revoked(session.getUserId(), session.getSessionId(), session.getSessionType()));
    }

    public void revokeAll(Long userId) {
        adapter.revokeAll(userId);
        events.publishEvent(new Revoked(userId, null, SessionScope.EITHER));
    }

    public static boolean isManager(Integer role) {
        return Integer.valueOf(0).equals(role) || Integer.valueOf(1).equals(role);
    }

    public static void checkUser(User user) {
        if (user == null || !Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new AuthAccessException(401, "账号不存在或已停用");
        }
        if (!isManager(user.getUserType()) && !Integer.valueOf(2).equals(user.getUserType())) {
            throw new AuthAccessException(403, "账号角色无效");
        }
    }

    public static String normalize(String token) {
        if (token == null) return null;
        String value = token.trim();
        return value.regionMatches(true, 0, "Bearer ", 0, 7) ? value.substring(7).trim() : value;
    }

    private static AuthAccessException expired() { return new AuthAccessException(401, "登录已失效，请重新登录"); }

    private static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest() : null;
    }
}
