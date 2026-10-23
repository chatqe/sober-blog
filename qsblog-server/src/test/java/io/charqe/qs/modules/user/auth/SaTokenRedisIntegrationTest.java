package io.charqe.qs.modules.user.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisTemplate;
import cn.dev33.satoken.json.SaJsonTemplate;
import cn.dev33.satoken.json.SaJsonTemplateForJackson;
import cn.dev33.satoken.stp.StpLogic;
import io.charqe.qs.common.config.DotenvEnvironmentPostProcessor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 仅显式启用时访问已授权的空 DB 15；不启动业务应用，不连接 MySQL，不创建真实用户。 */
@EnabledIfSystemProperty(named = "auth.redis.integration", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaTokenRedisIntegrationTest {
    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration(RedisAutoConfiguration.class)
    static class RedisOnlyConfiguration { }

    private AnnotationConfigApplicationContext context;
    private StringRedisTemplate redis;
    private TrackingDao nativeDao;
    private SaTokenDao previousDao;
    private SaJsonTemplate previousJson;
    private Map<String, StpLogic> previousDomains;
    private AuthSessionProperties properties;
    private SaTokenSessionAdapter adapter;
    private final Set<String> ownedKeys = ConcurrentHashMap.newKeySet();
    private final Map<ch.qos.logback.classic.Logger, ch.qos.logback.classic.Level> logLevels = new HashMap<>();

    @BeforeAll
    void connectToApprovedEmptyDatabase() throws Exception {
        for (String name : List.of(org.slf4j.Logger.ROOT_LOGGER_NAME, "io.lettuce.core")) {
            var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(name);
            logLevels.put(logger, logger.getLevel());
            logger.setLevel(ch.qos.logback.classic.Level.OFF);
        }
        assertEquals("15", System.getProperty("auth.redis.test-database"), "必须显式指定已授权的 DB 15");
        var environment = new StandardEnvironment();
        for (var source : new YamlPropertySourceLoader().load("redis-dev", new ClassPathResource("application-dev.yml"))) {
            environment.getPropertySources().addLast(source);
        }
        new DotenvEnvironmentPostProcessor().postProcessEnvironment(environment, null);
        String host = environment.getRequiredProperty("spring.redis.host");
        int port = environment.getRequiredProperty("spring.redis.port", Integer.class);
        assertEquals("192.168.30.100", host, "目标地址变化后必须重新确认测试授权");
        assertEquals(6379, port);
        assertNotEquals(15, environment.getRequiredProperty("spring.redis.database", Integer.class));
        Map<String, Object> connectionProperties = new HashMap<>();
        connectionProperties.put("spring.redis.host", host);
        connectionProperties.put("spring.redis.port", port);
        connectionProperties.put("spring.redis.database", 15);
        connectionProperties.put("spring.redis.password", environment.getRequiredProperty("spring.redis.password"));
        connectionProperties.put("spring.redis.ssl", environment.getProperty("spring.redis.ssl", Boolean.class, false));
        connectionProperties.put("spring.redis.timeout", "2s");
        connectionProperties.put("spring.redis.connect-timeout", "2s");
        for (String option : List.of("enabled", "max-active", "max-idle", "min-idle", "max-wait")) {
            String key = "spring.redis.lettuce.pool." + option;
            String value = environment.getProperty(key);
            if (value != null) connectionProperties.put(key, value);
        }
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("approved-redis-test", connectionProperties));
        context.register(RedisOnlyConfiguration.class);
        context.refresh();
        redis = context.getBean(StringRedisTemplate.class);
        try (var connection = redis.getConnectionFactory().getConnection()) {
            assertEquals("PONG", connection.ping());
            var server = connection.info("server");
            assertNotNull(server);
            assertTrue(Integer.parseInt(server.getProperty("redis_version").split("\\.")[0]) >= 6);
            assertEquals("standalone", server.getProperty("redis_mode"));
            assertEquals(0L, connection.dbSize(), "DB 15 非空，禁止测试写入和清库");
            System.out.printf("[Redis 集成] version=%s db=15 initialKeys=0 factory=%s%n",
                    server.getProperty("redis_version"), redis.getConnectionFactory().getClass().getSimpleName());
        }
    }

    @AfterAll
    void closeConnection() {
        try {
            if (redis != null) {
                try (var connection = redis.getConnectionFactory().getConnection()) {
                    System.out.printf("[Redis 集成清理] db=15 remainingKeys=%d%n", connection.dbSize());
                }
            }
        } finally {
            try {
                if (context != null) context.close();
            } finally {
                logLevels.forEach(ch.qos.logback.classic.Logger::setLevel);
            }
        }
    }

    @BeforeEach
    void setupNativeFramework() {
        try (var connection = redis.getConnectionFactory().getConnection()) {
            assertEquals(0L, connection.dbSize(), "测试之间 DB 15 必须为空；不会清理未知键");
        }
        previousDao = SaManager.getSaTokenDao();
        previousJson = SaManager.getSaJsonTemplate();
        // 最小容器不启动 Sa-Token 的插件生命周期，显式安装 starter 自带的同一 Jackson 实现。
        SaManager.setSaJsonTemplate(new SaJsonTemplateForJackson());
        previousDomains = new HashMap<>(SaManager.stpLogicMap);
        nativeDao = new TrackingDao();
        nativeDao.init(redis.getConnectionFactory());
        SaManager.setSaTokenDao(nativeDao);
        properties = new AuthSessionProperties();
        adapter = new SaTokenSessionAdapter(redis, properties);
    }

    @AfterEach
    void removeOnlyKeysCreatedByThisTest() {
        try {
            if (!ownedKeys.isEmpty()) redis.delete(ownedKeys);
            ownedKeys.clear();
            try (var connection = redis.getConnectionFactory().getConnection()) {
                assertEquals(0L, connection.dbSize(), "发现非本次记录的键，保留并停止后续测试");
            }
        } finally {
            if (previousDao != null) SaManager.setSaTokenDao(previousDao);
            if (previousJson != null) SaManager.setSaJsonTemplate(previousJson);
            if (previousDomains != null) {
                SaManager.stpLogicMap.clear();
                SaManager.stpLogicMap.putAll(previousDomains);
            }
        }
    }

    /** 只记录本次原生 DAO 创建的确切键，序列化和存取仍执行框架真实实现。 */
    private class TrackingDao extends SaTokenDaoForRedisTemplate {
        @Override
        public void set(String key, String value, long timeout) {
            ownedKeys.add(key);
            super.set(key, value, timeout);
        }
    }

    private long userId() {
        long id = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
        ownedKeys.add(SaTokenSessionAdapter.revisionKey(id));
        return id;
    }

    private SaTokenSessionAdapter.Metadata metadata(long userId, SessionScope scope) {
        var value = new SaTokenSessionAdapter.Metadata();
        long now = System.currentTimeMillis();
        value.setSessionId(UUID.randomUUID().toString());
        value.setUserId(userId);
        value.setSessionType(scope);
        value.setIssuedAt(now);
        value.setIdleExpiresAt(now + properties.policy(scope).getIdle().toMillis());
        value.setAbsoluteExpiresAt(now + properties.policy(scope).getAbsolute().toMillis());
        value.setRevision(adapter.revision(userId));
        return value;
    }

    private StpLogic logic(SessionScope scope) {
        return SaManager.getStpLogic(scope.name().toLowerCase(Locale.ROOT), false);
    }

    private long ttl(String key) {
        return Objects.requireNonNull(redis.getExpire(key, TimeUnit.MILLISECONDS));
    }

    private SaTokenSessionAdapter.Metadata resolve(String token, boolean touch) {
        var snapshot = adapter.find(token);
        return snapshot == null ? null : adapter.resolve(token, snapshot, System.currentTimeMillis(), touch,
                properties.policy(snapshot.getSessionType()).getIdle().toMillis());
    }

    private void await(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "等待 Redis 状态变化超时");
            Thread.sleep(50);
        }
    }

    private void race(Runnable first, Runnable second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            var a = executor.submit(() -> { assertTrue(start.await(5, TimeUnit.SECONDS)); first.run(); return null; });
            var b = executor.submit(() -> { assertTrue(start.await(5, TimeUnit.SECONDS)); second.run(); return null; });
            start.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void productionAutoConfigurationUsesLettucePoolAndNativeSerializationRoundTrips() {
        var factory = assertInstanceOf(LettuceConnectionFactory.class, context.getBean(RedisConnectionFactory.class));
        var pooling = assertInstanceOf(LettucePoolingClientConfiguration.class, factory.getClientConfiguration());
        assertEquals(25, pooling.getPoolConfig().getMaxTotal());
        assertEquals(15, factory.getDatabase());
        var expected = metadata(userId(), SessionScope.WEB);
        String token = adapter.issue(expected);
        assertNotNull(token);
        assertEquals(expected, adapter.find(token));
        assertNotNull(resolve(token, false));
        String key = logic(SessionScope.WEB).splicingKeyTokenSession(token);
        assertNotNull(redis.opsForValue().get(key));
        assertEquals(expected.getUserId().toString(), redis.opsForValue().get(logic(SessionScope.WEB).splicingKeyTokenValue(token)));
    }

    @Test
    void eachDomainKeepsItsConfiguredAbsoluteTtlOnNativeKeys() {
        for (var scope : List.of(SessionScope.WEB, SessionScope.ADMIN)) {
            String token = adapter.issue(metadata(userId(), scope));
            var logic = logic(scope);
            long expected = properties.policy(scope).getAbsolute().toMillis();
            for (String key : List.of(logic.splicingKeyTokenValue(token), logic.splicingKeyLastActiveTime(token), logic.splicingKeyTokenSession(token))) {
                assertTrue(ttl(key) > expected - 5_000 && ttl(key) <= expected, "原生键绝对 TTL 不符");
            }
        }
    }

    @Test
    void passiveReadsNeverChangeLastActiveOrExtendTtl() throws Exception {
        String token = adapter.issue(metadata(userId(), SessionScope.ADMIN));
        String key = logic(SessionScope.ADMIN).splicingKeyLastActiveTime(token);
        String before = redis.opsForValue().get(key);
        long beforeTtl = ttl(key);
        Thread.sleep(80);
        for (int i = 0; i < 5; i++) assertNotNull(resolve(token, false));
        assertEquals(before, redis.opsForValue().get(key));
        assertTrue(ttl(key) < beforeTtl);
    }

    @Test
    void activeLuaUpdatePreservesNativeSuffixAndAbsoluteTtl() throws Exception {
        String token = adapter.issue(metadata(userId(), SessionScope.WEB));
        String key = logic(SessionScope.WEB).splicingKeyLastActiveTime(token);
        String before = redis.opsForValue().get(key);
        long beforeTtl = ttl(key);
        Thread.sleep(80);
        var resolved = resolve(token, true);
        assertNotNull(resolved);
        String after = redis.opsForValue().get(key);
        assertTrue(Long.parseLong(after.split(",")[0]) > Long.parseLong(before.split(",")[0]));
        assertEquals(before.replaceFirst("^\\d+", ""), after.replaceFirst("^\\d+", ""));
        assertTrue(ttl(key) > 0 && ttl(key) < beforeTtl, "活跃更新不得去除或延长绝对 TTL");
        assertTrue(resolved.getIdleExpiresAt() <= resolved.getAbsoluteExpiresAt());
    }

    @Test
    void nativeSessionUpdatesKeepTtlAndNeverRecreateMissingRecords() throws Exception {
        String token = adapter.issue(metadata(userId(), SessionScope.ADMIN));
        String key = logic(SessionScope.ADMIN).splicingKeyTokenSession(token);
        var session = logic(SessionScope.ADMIN).getTokenSessionByToken(token, false);
        long beforeTtl = ttl(key);
        Thread.sleep(80);
        session.set("testMarker", "updated");
        assertTrue(ttl(key) > 0 && ttl(key) < beforeTtl, "框架原生更新必须支持 KEEPTTL");
        assertEquals("updated", logic(SessionScope.ADMIN).getTokenSessionByToken(token, false).getString("testMarker"));
        redis.delete(key);
        session.set("testMarker", "stale");
        assertFalse(redis.hasKey(key), "框架原生更新不得重建已删除的 Token-Session");
    }

    @Test
    void independentAccountsAndDevicesSurviveSelectedDomainLogout() {
        long user = userId();
        String web = adapter.issue(metadata(user, SessionScope.WEB));
        String device = adapter.issue(metadata(user, SessionScope.WEB));
        String admin = adapter.issue(metadata(userId(), SessionScope.ADMIN));
        assertEquals(3, Set.of(web, device, admin).size());
        adapter.delete(web, SessionScope.WEB);
        adapter.delete(web, SessionScope.WEB);
        assertNull(resolve(web, true));
        assertNotNull(resolve(device, false));
        assertNotNull(resolve(admin, false));
        assertNull(logic(SessionScope.ADMIN).getLoginIdByTokenNotThinkFreeze(device));
    }

    @Test
    void missingActiveKeyCannotBeRecreatedByStaleTouch() {
        String token = adapter.issue(metadata(userId(), SessionScope.ADMIN));
        var snapshot = adapter.find(token);
        String key = logic(SessionScope.ADMIN).splicingKeyLastActiveTime(token);
        redis.delete(key);
        assertNull(adapter.resolve(token, snapshot, System.currentTimeMillis(), true, properties.getAdmin().getIdle().toMillis()));
        assertFalse(redis.hasKey(key));
    }

    @Test
    void idleAndAbsoluteBoundariesRejectRatherThanRenew() {
        String token = adapter.issue(metadata(userId(), SessionScope.WEB));
        var snapshot = adapter.find(token);
        String key = logic(SessionScope.WEB).splicingKeyLastActiveTime(token);
        String before = redis.opsForValue().get(key);
        long last = Long.parseLong(before.split(",")[0]);
        long idle = properties.getWeb().getIdle().toMillis();
        assertNotNull(adapter.resolve(token, snapshot, last + idle - 1, false, idle));
        assertNull(adapter.resolve(token, snapshot, last + idle, true, idle));
        assertNull(adapter.resolve(token, snapshot, snapshot.getAbsoluteExpiresAt(), true, properties.getWeb().getAbsolute().toMillis()));
        assertEquals(before, redis.opsForValue().get(key));
    }

    @Test
    void realIdleExpiryOnlyInvalidatesShorterDomainAndRetainsNativeRecord() throws Exception {
        properties.setAdmin(new AuthSessionProperties.Policy(Duration.ofSeconds(2), Duration.ofSeconds(10)));
        properties.setWeb(new AuthSessionProperties.Policy(Duration.ofSeconds(8), Duration.ofSeconds(12)));
        adapter = new SaTokenSessionAdapter(redis, properties);
        long user = userId();
        String admin = adapter.issue(metadata(user, SessionScope.ADMIN));
        String web = adapter.issue(metadata(user, SessionScope.WEB));
        assertNotNull(resolve(admin, false));
        await(() -> resolve(admin, false) == null, Duration.ofSeconds(5));
        assertNull(resolve(admin, true));
        assertNotNull(resolve(web, false));
        assertTrue(redis.hasKey(logic(SessionScope.ADMIN).splicingKeyTokenValue(admin)));
    }

    @Test
    void realAbsoluteExpiryWinsEvenWithContinuousActivity() throws Exception {
        properties.setAdmin(new AuthSessionProperties.Policy(Duration.ofSeconds(2), Duration.ofSeconds(3)));
        adapter = new SaTokenSessionAdapter(redis, properties);
        String token = adapter.issue(metadata(userId(), SessionScope.ADMIN));
        String key = logic(SessionScope.ADMIN).splicingKeyTokenValue(token);
        long deadline = System.nanoTime() + Duration.ofSeconds(6).toNanos();
        int touches = 0;
        while (resolve(token, true) != null) {
            touches++;
            assertTrue(System.nanoTime() < deadline, "活跃请求错误延长了绝对寿命");
            Thread.sleep(150);
        }
        assertTrue(touches >= 2);
        await(() -> ttl(key) == -2, Duration.ofSeconds(2));
        assertNull(adapter.find(token));
        await(() -> !redis.hasKey(logic(SessionScope.ADMIN).splicingKeyLastActiveTime(token))
                && !redis.hasKey(logic(SessionScope.ADMIN).splicingKeyTokenSession(token)), Duration.ofSeconds(2));
    }

    @Test
    void globalRevocationRejectsBothDomainsButPreservesNewLoginAndOtherUsers() {
        long user = userId();
        var oldCredentials = metadata(user, SessionScope.WEB);
        String web = adapter.issue(oldCredentials);
        String admin = adapter.issue(metadata(user, SessionScope.ADMIN));
        String other = adapter.issue(metadata(userId(), SessionScope.WEB));
        adapter.revokeAll(user);
        assertEquals(1, adapter.revision(user));
        assertNull(resolve(web, true));
        assertNull(resolve(admin, false));
        assertNotNull(resolve(other, false));
        assertNull(adapter.issue(oldCredentials));
        String newer = adapter.issue(metadata(user, SessionScope.WEB));
        assertNotNull(resolve(newer, false));
        assertNotNull(adapter.find(web));
    }

    @Test
    void concurrentLogoutAndTouchNeverResurrectNativeKeys() throws Exception {
        for (var scope : List.of(SessionScope.WEB, SessionScope.ADMIN)) {
            for (int i = 0; i < 8; i++) {
                String token = adapter.issue(metadata(userId(), scope));
                var snapshot = adapter.find(token);
                race(() -> adapter.resolve(token, snapshot, System.currentTimeMillis(), true, properties.policy(scope).getIdle().toMillis()),
                        () -> adapter.delete(token, scope));
                assertNull(resolve(token, true));
                assertFalse(redis.hasKey(logic(scope).splicingKeyTokenValue(token)));
                assertFalse(redis.hasKey(logic(scope).splicingKeyLastActiveTime(token)));
                assertFalse(redis.hasKey(logic(scope).splicingKeyTokenSession(token)));
            }
        }
    }

    @Test
    void concurrentRevocationAndTouchAlwaysRejectOldRevisionAfterCompletion() throws Exception {
        for (int i = 0; i < 12; i++) {
            long user = userId();
            String token = adapter.issue(metadata(user, SessionScope.WEB));
            var snapshot = adapter.find(token);
            race(() -> adapter.resolve(token, snapshot, System.currentTimeMillis(), true, properties.getWeb().getIdle().toMillis()),
                    () -> adapter.revokeAll(user));
            assertNull(resolve(token, true));
            assertNotNull(resolve(adapter.issue(metadata(user, SessionScope.WEB)), false));
        }
    }

    @Test
    void revocationDuringIssuanceCleansOldTokenWithoutKickingPostEventLogin() throws Exception {
        for (var scope : List.of(SessionScope.WEB, SessionScope.ADMIN)) {
            long user = userId();
            var oldCredentials = metadata(user, scope);
            var delayed = spy(adapter);
            var firstRead = new AtomicBoolean(true);
            var observed = new CountDownLatch(1);
            var proceed = new CountDownLatch(1);
            // 只控制调度点，版本读取、签发和撤销全部访问真实 Redis。
            doAnswer(invocation -> {
                long revision = (long) invocation.callRealMethod();
                if (firstRead.getAndSet(false)) {
                    observed.countDown();
                    assertTrue(proceed.await(10, TimeUnit.SECONDS));
                }
                return revision;
            }).when(delayed).revision(user);
            var executor = Executors.newSingleThreadExecutor();
            try {
                var oldIssue = executor.submit(() -> delayed.issue(oldCredentials));
                assertTrue(observed.await(5, TimeUnit.SECONDS));
                adapter.revokeAll(user);
                String newer = adapter.issue(metadata(user, scope));
                proceed.countDown();
                assertNull(oldIssue.get(10, TimeUnit.SECONDS));
                assertNotNull(resolve(newer, true));
                assertEquals(List.of(newer), logic(scope).getTokenValueListByLoginId(user));
            } finally {
                proceed.countDown();
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }
}
