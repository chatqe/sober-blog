package io.charqe.qs.modules.user.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.StpLogic;
import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.config.DotenvEnvironmentPostProcessor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 执行真实 Sa-Token 内核和内存 DAO；只替换撤销版本读取，不冒充 Redis/Lua 集成测试。 */
class SaTokenSessionAdapterTest {
    private SaTokenDao previousDao;
    private Map<String, StpLogic> previousDomains;
    private SaTokenDaoDefaultImpl nativeDao;
    private ValueOperations<String, String> values;
    private SaTokenSessionAdapter adapter;
    private final AuthSessionProperties properties = new AuthSessionProperties();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        previousDao = SaManager.getSaTokenDao();
        previousDomains = new HashMap<>(SaManager.stpLogicMap);
        nativeDao = spy(new SaTokenDaoDefaultImpl());
        SaManager.setSaTokenDao(nativeDao);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        adapter = new SaTokenSessionAdapter(redis, properties);
    }

    @AfterEach
    void cleanup() {
        SaManager.setSaTokenDao(previousDao);
        nativeDao.destroy();
        SaManager.stpLogicMap.clear();
        SaManager.stpLogicMap.putAll(previousDomains);
    }

    private SaTokenSessionAdapter.Metadata metadata(long userId, SessionScope scope) {
        var metadata = new SaTokenSessionAdapter.Metadata();
        long now = System.currentTimeMillis();
        metadata.setSessionId(UUID.randomUUID().toString());
        metadata.setUserId(userId);
        metadata.setSessionType(scope);
        metadata.setIssuedAt(now);
        metadata.setIdleExpiresAt(now + properties.policy(scope).getIdle().toMillis());
        metadata.setAbsoluteExpiresAt(now + properties.policy(scope).getAbsolute().toMillis());
        metadata.setRevision(0);
        return metadata;
    }

    private StpLogic logic(SessionScope scope) {
        return SaManager.getStpLogic(scope.name().toLowerCase(java.util.Locale.ROOT), false);
    }

    /** 显式启用的只读预检：不启动应用、不签发会话，也不读写任何业务键值。 */
    @Test
    @EnabledIfSystemProperty(named = "auth.redis.preflight", matches = "true")
    void devRedisReadOnlyPreflight() throws Exception {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("io.lettuce.core");
        var rootLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        var previousLevel = logger.getLevel();
        var previousRootLevel = rootLogger.getLevel();
        // 禁止客户端协议日志输出认证命令；诊断只输出白名单信息。
        logger.setLevel(ch.qos.logback.classic.Level.OFF);
        rootLogger.setLevel(ch.qos.logback.classic.Level.WARN);
        RedisClient client = null;
        try {
            var environment = new StandardEnvironment();
            for (var source : new YamlPropertySourceLoader().load("redis-dev",
                    new ClassPathResource("application-dev.yml"))) {
                environment.getPropertySources().addLast(source);
            }
            new DotenvEnvironmentPostProcessor().postProcessEnvironment(environment, null);
            String host = environment.getRequiredProperty("spring.redis.host");
            int port = environment.getRequiredProperty("spring.redis.port", Integer.class);
            int database = environment.getRequiredProperty("spring.redis.database", Integer.class);
            String password = environment.getRequiredProperty("spring.redis.password");
            boolean ssl = environment.getProperty("spring.redis.ssl", Boolean.class, false);
            var uri = RedisURI.Builder.redis(host, port).withDatabase(database)
                    .withPassword(password.toCharArray()).withSsl(ssl)
                    .withTimeout(Duration.ofSeconds(2)).build();
            client = RedisClient.create(uri);
            try (var connection = client.connect()) {
                var commands = connection.sync();
                assertEquals("PONG", commands.ping());
                var server = new java.util.Properties();
                server.load(new java.io.StringReader(commands.info("server")));
                String version = server.getProperty("redis_version");
                String mode = server.getProperty("redis_mode");
                assertNotNull(version, "Redis 未返回版本信息");
                System.out.printf("[Redis 只读预检] %s:%d db=%d PING=PONG version=%s mode=%s%n",
                        host, port, database, version, mode);
                commands.info("keyspace").lines()
                        .filter(line -> line.matches("db\\d+:keys=\\d+,expires=\\d+,avg_ttl=\\d+.*"))
                        .forEach(line -> System.out.println("[Redis 库占用] " + line));
                assertTrue(Integer.parseInt(version.split("\\.")[0]) >= 6,
                        "会话原子脚本要求 Redis 6 或更新版本");
                assertNotEquals("cluster", mode, "当前脚本尚未适配 Redis Cluster 跨槽");
            }
        } catch (RuntimeException error) {
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            String message = String.valueOf(cause.getMessage());
            String reason = message.contains("WRONGPASS") ? "认证失败"
                    : message.contains("NOAUTH") ? "需要认证"
                    : message.contains("NOPERM") ? "ACL 权限不足" : cause.getClass().getSimpleName();
            fail("Redis 只读预检失败：" + reason);
        } finally {
            try {
                if (client != null) client.shutdown();
            } finally {
                logger.setLevel(previousLevel);
                rootLogger.setLevel(previousRootLevel);
            }
        }
    }

    @Test
    void everyLoginGetsAnIndependentNativeTokenAcrossDomainsAndDevices() {
        String web = adapter.issue(metadata(1, SessionScope.WEB));
        String anotherDevice = adapter.issue(metadata(1, SessionScope.WEB));
        String admin = adapter.issue(metadata(2, SessionScope.ADMIN));
        assertEquals(3, Set.of(web, anotherDevice, admin).size());
        for (String token : Set.of(web, anotherDevice, admin)) assertTrue(SaTokenSessionAdapter.validFormat(token));
        assertEquals(1L, adapter.find(web).getUserId());
        assertEquals(2L, adapter.find(admin).getUserId());
        assertNull(logic(SessionScope.ADMIN).getLoginIdByTokenNotThinkFreeze(web));
        assertNull(logic(SessionScope.WEB).getLoginIdByTokenNotThinkFreeze(admin));
        assertEquals(2, logic(SessionScope.WEB).getTokenValueListByLoginId(1L).size());
    }

    @Test
    void nativeKeysAndTtlsMatchEachDomainsAbsoluteLifetime() {
        for (SessionScope scope : new SessionScope[]{SessionScope.WEB, SessionScope.ADMIN}) {
            long start = System.currentTimeMillis();
            String token = adapter.issue(metadata(1, scope));
            StpLogic logic = logic(scope);
            String prefix = "auth:v2:" + scope.name().toLowerCase(java.util.Locale.ROOT) + ":";
            assertTrue(logic.splicingKeyTokenValue(token).startsWith(prefix));
            assertEquals("1", nativeDao.get(logic.splicingKeyTokenValue(token)));
            long absolute = properties.policy(scope).getAbsolute().getSeconds();
            for (String key : new String[]{logic.splicingKeyTokenValue(token),
                    logic.splicingKeyLastActiveTime(token), logic.splicingKeyTokenSession(token)}) {
                long ttl = nativeDao.getObjectTimeout(key);
                assertTrue(ttl > absolute - 5 && ttl <= absolute, key + " TTL=" + ttl);
            }
            String active = nativeDao.get(logic.splicingKeyLastActiveTime(token));
            long last = Long.parseLong(active.split(",")[0]);
            assertTrue(last >= start && last <= System.currentTimeMillis());
            assertEquals(properties.policy(scope).getIdle().getSeconds(), logic.getTokenUseActiveTimeoutOrGlobalConfig(token));
            assertFalse(logic.getConfig().getAutoRenew());
            assertFalse(logic.getConfig().getIsReadCookie());
            assertFalse(logic.getConfig().getIsReadHeader());
            assertFalse(logic.getConfig().getIsReadBody());
        }
    }

    @Test
    void tokenSessionOnlyStoresWhitelistedMetadataAndReadsDoNotRenew() {
        var expected = metadata(1, SessionScope.WEB);
        String token = adapter.issue(expected);
        StpLogic logic = logic(SessionScope.WEB);
        var nativeSession = logic.getTokenSessionByToken(token, false);
        var json = JSON.parseObject(nativeSession.getString("auth"));
        assertEquals(Set.of("sessionId", "userId", "sessionType", "issuedAt", "idleExpiresAt", "absoluteExpiresAt", "revision"), json.keySet());
        assertFalse(json.toJSONString().contains(token));
        String before = nativeDao.get(logic.splicingKeyLastActiveTime(token));
        long ttl = nativeDao.getTimeout(logic.splicingKeyTokenValue(token));
        clearInvocations(nativeDao);
        assertEquals(expected, adapter.find(token));
        assertEquals(before, nativeDao.get(logic.splicingKeyLastActiveTime(token)));
        assertTrue(nativeDao.getTimeout(logic.splicingKeyTokenValue(token)) <= ttl);
        verify(nativeDao, never()).setObject(anyString(), any(), anyLong());
        verify(nativeDao, never()).updateObject(anyString(), any());
        verify(nativeDao, never()).updateObjectTimeout(anyString(), anyLong());
    }

    @Test
    void nativeLogoutOnlyRemovesSelectedTokenAndNeverRecreatesIt() {
        String web = adapter.issue(metadata(1, SessionScope.WEB));
        String anotherDevice = adapter.issue(metadata(1, SessionScope.WEB));
        String admin = adapter.issue(metadata(1, SessionScope.ADMIN));
        adapter.delete(web, SessionScope.WEB);
        adapter.delete(web, SessionScope.WEB);
        assertNull(adapter.find(web));
        assertNull(nativeDao.get(logic(SessionScope.WEB).splicingKeyLastActiveTime(web)));
        assertNull(nativeDao.getObject(logic(SessionScope.WEB).splicingKeyTokenSession(web)));
        assertNotNull(adapter.find(anotherDevice));
        assertNotNull(adapter.find(admin));
    }

    @Test
    void missingNativeTokenSessionDoesNotGetCreatedOnRead() {
        String token = adapter.issue(metadata(1, SessionScope.WEB));
        String key = logic(SessionScope.WEB).splicingKeyTokenSession(token);
        nativeDao.deleteObject(key);
        assertNull(adapter.find(token));
        assertNull(nativeDao.getObject(key));
        assertNull(adapter.find("USER_legacy"));
        assertNull(adapter.find("x".repeat(64)));
    }

    @Test
    void revocationBeforeIssuanceDoesNotCreateNativeRecords() {
        when(values.get(SaTokenSessionAdapter.revisionKey(1L))).thenReturn("1");
        assertNull(adapter.issue(metadata(1, SessionScope.ADMIN)));
        verify(nativeDao, never()).setObject(anyString(), any(), anyLong());
    }

    @Test
    void revocationDuringIssuanceDeletesJustCreatedSessionButNotNewerLogin() {
        String revisionKey = SaTokenSessionAdapter.revisionKey(1L);
        when(values.get(revisionKey)).thenReturn("0", "1");
        assertNull(adapter.issue(metadata(1, SessionScope.WEB)));
        assertTrue(logic(SessionScope.WEB).searchTokenValue("", 0, -1, false).isEmpty());
        assertTrue(logic(SessionScope.WEB).searchTokenSessionId("", 0, -1, false).isEmpty());
        var newer = metadata(1, SessionScope.WEB);
        newer.setRevision(1);
        String token = adapter.issue(newer);
        assertNotNull(token);
        assertEquals(1, adapter.find(token).getRevision());
    }

    @Test
    void failureAfterNativeIssuanceCleansUpTheNewToken() {
        when(values.get(SaTokenSessionAdapter.revisionKey(1L)))
                .thenReturn("0").thenThrow(new IllegalStateException("模拟撤销版本存储故障"));
        assertThrows(IllegalStateException.class, () -> adapter.issue(metadata(1, SessionScope.ADMIN)));
        assertTrue(logic(SessionScope.ADMIN).searchTokenValue("", 0, -1, false).isEmpty());
        assertTrue(logic(SessionScope.ADMIN).searchTokenSessionId("", 0, -1, false).isEmpty());
    }

    @Test
    void globalRevocationOnlyIncrementsRevisionWithoutEnumeratingOrKickingNewSessions() {
        String web = adapter.issue(metadata(1, SessionScope.WEB));
        String admin = adapter.issue(metadata(1, SessionScope.ADMIN));
        clearInvocations(nativeDao);
        adapter.revokeAll(1L);
        verify(values).increment(SaTokenSessionAdapter.revisionKey(1L));
        verify(nativeDao, never()).searchData(anyString(), anyString(), anyInt(), anyInt(), anyBoolean());
        verify(nativeDao, never()).deleteObject(anyString());
        // find 只读元数据，撤销有效性由 resolve 的原子脚本判定。
        assertNotNull(adapter.find(web));
        assertNotNull(adapter.find(admin));
    }
}
