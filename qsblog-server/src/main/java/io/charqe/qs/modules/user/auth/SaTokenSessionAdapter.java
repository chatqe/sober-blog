package io.charqe.qs.modules.user.auth;

import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpLogic;
import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 会话签发、存储与删除交给 Sa-Token，仅补充项目的撤销版本和原子活动校验。 */
@Component
public class SaTokenSessionAdapter {
    private final StringRedisTemplate redis;
    private final Map<SessionScope, StpLogic> domains = new EnumMap<>(SessionScope.class);

    /** 原生 Token-Session 的业务元数据，不包含账号快照、密码或另一份 token。 */
    @Data
    public static class Metadata {
        private String sessionId;
        private Long userId;
        private SessionScope sessionType;
        private long issuedAt;
        private long idleExpiresAt;
        private long absoluteExpiresAt;
        private long revision;
    }

    public SaTokenSessionAdapter(StringRedisTemplate redis, AuthSessionProperties properties) {
        this.redis = redis;
        properties.validate();
        for (SessionScope scope : List.of(SessionScope.WEB, SessionScope.ADMIN)) {
            AuthSessionProperties.Policy policy = properties.policy(scope);
            SaTokenConfig config = new SaTokenConfig()
                    .setTokenName("auth:v2")
                    .setTimeout(policy.getAbsolute().getSeconds())
                    .setActiveTimeout(policy.getIdle().getSeconds())
                    .setAutoRenew(false).setIsConcurrent(true).setIsShare(false)
                    .setIsReadCookie(false).setIsReadBody(false).setIsReadHeader(false)
                    .setIsWriteHeader(false).setTokenStyle("random-64");
            domains.put(scope, new StpLogic(scope.name().toLowerCase(Locale.ROOT)).setConfig(config));
        }
    }

    // 绑定 Sa-Token 1.46.0 活跃时间格式与 Redis 6+，使用单节点/Sentinel，非 Cluster 跨槽脚本。
    // 只更新尚存且未过期的原生键，保留绝对 TTL；框架升级时必须运行适配契约测试。
    static final String RESOLVE = """
        if redis.call('GET', KEYS[1]) ~= ARGV[1] then return nil end
        if (redis.call('GET', KEYS[3]) or '0') ~= ARGV[2] then return nil end
        local raw = redis.call('GET', KEYS[2])
        if not raw then return nil end
        local last = tonumber(string.match(raw, '^%d+'))
        local now = tonumber(ARGV[3])
        if not last or last + tonumber(ARGV[4]) <= now or tonumber(ARGV[5]) <= now then
          return nil
        end
        if ARGV[6] == '1' then
          last = math.max(last, now)
          local suffix = string.match(raw, ',.*$') or ''
          redis.call('SET', KEYS[2], tostring(last) .. suffix, 'XX', 'KEEPTTL')
        end
        return last
        """;
    private static final DefaultRedisScript<Long> RESOLVE_SCRIPT = new DefaultRedisScript<>(RESOLVE, Long.class);

    long revision(Long userId) {
        String value = redis.opsForValue().get(revisionKey(userId));
        return value == null ? 0 : Long.parseLong(value);
    }

    String issue(Metadata session) {
        if (revision(session.getUserId()) != session.getRevision()) return null;
        StpLogic logic = domains.get(session.getSessionType());
        String token = logic.createLoginSession(session.getUserId(),
                logic.createSaLoginParameter().setRightNowCreateTokenSession(true));
        try {
            logic.getTokenSessionByToken(token, false).set("auth", JSON.toJSONString(session));
            // 签发中发生改密或降权时，刚签发的旧凭据会话也不可使用。
            if (revision(session.getUserId()) != session.getRevision()) {
                logic.logoutByTokenValue(token);
                return null;
            }
            return token;
        } catch (RuntimeException error) {
            logic.logoutByTokenValue(token);
            throw error;
        }
    }

    // 此方法只取元数据，业务不得用它替代 AuthSessionService 的完整校验。
    Metadata find(String token) {
        if (!validFormat(token)) return null;
        for (Map.Entry<SessionScope, StpLogic> entry : domains.entrySet()) {
            StpLogic logic = entry.getValue();
            Object userId = logic.getLoginIdByTokenNotThinkFreeze(token);
            if (userId == null) continue;
            SaSession nativeSession = logic.getTokenSessionByToken(token, false);
            if (nativeSession == null) continue;
            Metadata session = JSON.parseObject(nativeSession.getString("auth"), Metadata.class);
            if (session != null && session.getSessionType() == entry.getKey()
                    && String.valueOf(session.getUserId()).equals(userId.toString())) return session;
        }
        return null;
    }

    Metadata resolve(String token, Metadata snapshot, long now, boolean touch, long idle) {
        StpLogic logic = domains.get(snapshot.getSessionType());
        if (logic == null || !validFormat(token)) return null;
        Long last = redis.execute(RESOLVE_SCRIPT,
                List.of(logic.splicingKeyTokenValue(token), logic.splicingKeyLastActiveTime(token),
                        revisionKey(snapshot.getUserId())),
                snapshot.getUserId().toString(), Long.toString(snapshot.getRevision()),
                Long.toString(now), Long.toString(idle), Long.toString(snapshot.getAbsoluteExpiresAt()),
                touch ? "1" : "0");
        if (last == null) return null;
        snapshot.setIdleExpiresAt(Math.min(last + idle, snapshot.getAbsoluteExpiresAt()));
        return snapshot;
    }

    void delete(String token, SessionScope scope) {
        if (validFormat(token)) domains.get(scope).logoutByTokenValue(token);
    }

    // 版本递增立即废止两域旧会话；原生记录按绝对 TTL 清理，避免枚举踢出时误踢新登录。
    void revokeAll(Long userId) { redis.opsForValue().increment(revisionKey(userId)); }

    static String revisionKey(Long userId) { return "auth:v2:revision:" + userId; }
    static boolean validFormat(String token) { return token != null && token.matches("[A-Za-z0-9]{64}"); }
}
