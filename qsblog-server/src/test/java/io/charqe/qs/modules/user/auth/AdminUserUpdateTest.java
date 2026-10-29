package io.charqe.qs.modules.user.auth;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.user.controller.admin.AdminUserController;
import io.charqe.qs.modules.user.controller.admin.vo.AdminUserUpdateReq;
import io.charqe.qs.modules.user.domain.dao.UserMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserServiceImpl;
import io.charqe.qs.utils.cache.QingSiCache;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 纯单元测试：覆盖实际编辑服务与旧入口，不连接业务数据库、Redis。 */
class AdminUserUpdateTest {
    private static ValidatorFactory validators;
    private UserMapper users;
    private AuthSessionService sessions;
    private QingSiCache cache;
    private UserServiceImpl service;

    @BeforeAll
    static void initialize() {
        validators = Validation.buildDefaultValidatorFactory();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "user-edit-test"), User.class);
    }

    @AfterAll
    static void close() { validators.close(); }

    @BeforeEach
    void setup() {
        users = mock(UserMapper.class);
        sessions = mock(AuthSessionService.class);
        cache = mock(QingSiCache.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", users);
        ReflectionTestUtils.setField(service, "sessions", sessions);
        ReflectionTestUtils.setField(service, "qingSiCache", cache);
        ReflectionTestUtils.setField(service, "validator", validators.getValidator());
        when(users.updateById(any(User.class))).thenReturn(1);
        // 逻辑删除带审计填充时，MyBatis-Plus 调用实体重载而非 Serializable 重载。
        when(users.deleteById(any(User.class))).thenReturn(1);
        when(users.selectCount(any())).thenReturn(0L);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private User user(long id, Integer role) {
        User user = new User();
        user.setId(id);
        user.setUserType(role);
        user.setUserStatus(1);
        user.setUsername("用户" + id);
        return user;
    }

    private void identity(User operator) {
        var metadata = new SaTokenSessionAdapter.Metadata();
        metadata.setSessionType(SessionScope.ADMIN);
        when(sessions.current(SessionScope.ADMIN, false))
                .thenReturn(new AuthSessionService.Resolved(metadata, operator, "test"));
    }

    private void prepare(int operatorRole, int targetRole) {
        User operator = user(11, operatorRole);
        identity(operator);
        when(users.selectList(any())).thenReturn(List.of(operator, user(22, targetRole)));
    }

    private AdminUserUpdateReq request() {
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(22L);
        request.setIntroduction("新简介");
        return request;
    }

    @ParameterizedTest
    @CsvSource({"0,0,true", "0,1,true", "0,2,true", "1,0,false", "1,1,true", "1,2,true",
            "2,0,false", "2,1,false", "2,2,false"})
    void enforcesOperatorAndTargetMatrix(int operatorRole, int targetRole, boolean allowed) {
        prepare(operatorRole, targetRole);
        if (allowed) {
            assertFalse(service.updateAdminUser(request()).getData());
            verify(users).updateById(any(User.class));
            verify(sessions, never()).revokeAll(anyLong());
        } else {
            assertEquals(403, assertThrows(AuthAccessException.class,
                    () -> service.updateAdminUser(request())).getStatus());
            verify(users, never()).updateById(any(User.class));
            verifyNoInteractions(cache);
        }
    }

    @ParameterizedTest
    @CsvSource({"0,0,true", "0,1,true", "0,2,true", "1,0,false", "1,1,true", "1,2,true"})
    void enforcesAssignableRoles(int operatorRole, int newRole, boolean allowed) {
        prepare(operatorRole, 2);
        AdminUserUpdateReq request = request();
        request.setUserType(newRole);
        if (allowed) {
            assertEquals(newRole != 2, service.updateAdminUser(request).getData());
            verify(sessions, times(newRole != 2 ? 1 : 0)).revokeAll(22L);
        } else {
            assertThrows(AuthAccessException.class, () -> service.updateAdminUser(request));
            verify(users, never()).updateById(any(User.class));
        }
    }

    @Test
    void adminCannotEscalateSelfToBoss() {
        User admin = user(11, 1);
        identity(admin);
        when(users.selectList(any())).thenReturn(List.of(admin));
        AdminUserUpdateReq request = request();
        request.setId(11L);
        request.setUserType(0);
        assertThrows(AuthAccessException.class, () -> service.updateAdminUser(request));
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void bossCanEditSelfAndClearOptionalFieldsWithoutTouchingCredentials() {
        User boss = user(11, 0);
        boss.setPhoneNumber("13800138000");
        boss.setEmail("owner@example.com");
        identity(boss);
        when(users.selectList(any())).thenReturn(List.of(boss));
        AdminUserUpdateReq request = request();
        request.setId(11L);
        request.setUsername("  新用户名  ");
        request.setAvatar("");
        request.setAdmire("");
        request.setIntroduction("");
        request.setGender(2);
        assertFalse(service.updateAdminUser(request).getData());
        ArgumentCaptor<User> update = ArgumentCaptor.forClass(User.class);
        verify(users).updateById(update.capture());
        User patch = update.getValue();
        assertEquals(11L, patch.getId());
        assertEquals("新用户名", patch.getUsername());
        assertEquals("", patch.getAvatar());
        assertEquals("", patch.getAdmire());
        assertEquals("", patch.getIntroduction());
        assertEquals(2, patch.getGender());
        assertNull(patch.getPassword());
        assertNull(patch.getEmail());
        assertNull(patch.getPhoneNumber());
        assertNull(patch.getOpenId());
        assertNull(patch.getUserType());
        verify(cache).del(CommonConst.ADMIN);
        verify(cache).del(CommonConst.USER_CACHE + 11L);
    }

    @ParameterizedTest
    @CsvSource({"0,0,true", "0,1,true", "0,2,true", "1,0,false", "1,1,true", "1,2,true",
            "2,0,false", "2,1,false", "2,2,false"})
    void contactUpdatesUseSamePermissions(int operatorRole, int targetRole, boolean allowed) {
        prepare(operatorRole, targetRole);
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(22L);
        request.setPhoneNumber(" 13800138000 ");
        request.setEmail(" user@example.com ");
        if (allowed) {
            assertTrue(service.updateAdminUser(request).getData());
            verify(users).updateById(argThat(patch -> "13800138000".equals(patch.getPhoneNumber())
                    && "user@example.com".equals(patch.getEmail()) && patch.getPassword() == null));
            verify(sessions).revokeAll(22L);
        } else {
            assertThrows(AuthAccessException.class, () -> service.updateAdminUser(request));
            verify(users, never()).updateById(any(User.class));
            verify(sessions, never()).revokeAll(anyLong());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void duplicateContactsExcludeSelfAndRejectWithoutInvalidation(boolean phone) {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        if (phone) request.setPhoneNumber("13800138000");
        else request.setEmail("user@example.com");
        when(users.selectCount(any())).thenReturn(1L);
        assertEquals(phone ? "手机号已被其他用户使用" : "邮箱已被其他用户使用",
                assertThrows(BizException.class, () -> service.updateAdminUser(request)).getMessage());
        verify(users).selectCount(argThat(wrapper -> wrapper.getSqlSegment().contains(phone ? "phone_number =" : "email =")
                && wrapper.getSqlSegment().contains("id <>")));
        verify(users, never()).updateById(any(User.class));
        verify(sessions, never()).revokeAll(anyLong());
        verifyNoInteractions(cache);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345678901", "+8613800138000", "138001380000", "1380013800a"})
    void rejectsInvalidPhonesAtService(String value) {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        request.setPhoneNumber(value);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verify(users, never()).updateById(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "user@", "user@localhost", "a b@example.com", "a..b@example.com",
            "123456789012345678901@example.comx"})
    void rejectsInvalidEmailsAtService(String value) {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        request.setEmail(value);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verify(users, never()).updateById(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void contactChangesRevokeOnlyAfterCommit(boolean phone) {
        prepare(1, 2);
        AdminUserUpdateReq request = request();
        if (phone) request.setPhoneNumber("13800138000");
        else request.setEmail("user@example.com");
        TransactionSynchronizationManager.initSynchronization();
        assertTrue(service.updateAdminUser(request).getData());
        verify(sessions, never()).revokeAll(anyLong());
        verifyNoInteractions(cache);
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
        verify(sessions).revokeAll(22L);
    }

    @Test
    void unchangedContactsAndEmptyUnboundContactsDoNotRevoke() {
        User boss = user(11, 0);
        boss.setPhoneNumber("13800138000");
        boss.setEmail("user@example.com");
        identity(boss);
        when(users.selectList(any())).thenReturn(List.of(boss));
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(11L);
        request.setPhoneNumber(" 13800138000 ");
        request.setEmail(" user@example.com ");
        assertFalse(service.updateAdminUser(request).getData());
        boss.setPhoneNumber(null);
        boss.setEmail(null);
        request.setPhoneNumber("");
        request.setEmail("  ");
        assertFalse(service.updateAdminUser(request).getData());
        verify(sessions, never()).revokeAll(anyLong());
    }

    @Test
    void clearingExistingContactsWritesEmptyStringsAndRevokes() {
        User boss = user(11, 0);
        boss.setPhoneNumber("13800138000");
        boss.setEmail("user@example.com");
        identity(boss);
        when(users.selectList(any())).thenReturn(List.of(boss));
        AdminUserUpdateReq request = new AdminUserUpdateReq();
        request.setId(11L);
        request.setPhoneNumber("");
        request.setEmail("");
        assertTrue(service.updateAdminUser(request).getData());
        verify(users).updateById(argThat(patch -> "".equals(patch.getPhoneNumber()) && "".equals(patch.getEmail())));
        verify(users, never()).selectCount(any());
        verify(sessions).revokeAll(11L);
    }

    @Test
    void failedContactSaveDoesNotRevoke() {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        request.setEmail("user@example.com");
        when(users.updateById(any(User.class))).thenReturn(0);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verify(sessions, never()).revokeAll(anyLong());
        verifyNoInteractions(cache);
    }

    @Test
    void rechecksLatestOperatorAndTargetRolesUnderOrderedLock() {
        prepare(1, 2);
        when(users.selectList(any())).thenReturn(List.of(user(11, 1), user(22, 0)));
        assertThrows(AuthAccessException.class, () -> service.updateAdminUser(request()));
        when(users.selectList(any())).thenReturn(List.of(user(11, 2), user(22, 2)));
        assertThrows(AuthAccessException.class, () -> service.updateAdminUser(request()));
        verify(users, never()).updateById(any(User.class));
        verify(users, times(2)).selectList(argThat(wrapper ->
                wrapper.getSqlSegment().contains("ORDER BY id ASC FOR UPDATE")));
    }

    @Test
    void rejectsMissingTargetAndWrongSessionDomain() {
        prepare(0, 2);
        when(users.selectList(any())).thenReturn(List.of(user(11, 0)));
        assertEquals("404", assertThrows(BizException.class, () -> service.updateAdminUser(request())).getCode());
        when(sessions.current(SessionScope.ADMIN, false)).thenThrow(new AuthAccessException(403, "会话类型不符"));
        assertThrows(AuthAccessException.class, () -> service.updateAdminUser(request()));
        verify(users, never()).updateById(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3, 99})
    void rejectsInvalidRoleAndStatusCodes(int value) {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        request.setUserType(value);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        request.setUserType(2);
        request.setUserStatus(value);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        request.setUserStatus(1);
        request.setGender(value);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verify(users, never()).updateById(any(User.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12345678901", "a@b", "12345678901234567890123456789012345"})
    void rejectsInvalidUsernames(String username) {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        request.setUsername(username);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void rejectsDuplicateUsernameAndDatabaseUniqueConflict() {
        prepare(0, 2);
        AdminUserUpdateReq request = request();
        request.setUsername("重复用户名");
        when(users.selectCount(any())).thenReturn(1L);
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verify(users, never()).updateById(any(User.class));
        when(users.selectCount(any())).thenReturn(0L);
        when(users.updateById(any(User.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(BizException.class, () -> service.updateAdminUser(request));
        verifyNoInteractions(cache);
        verify(sessions, never()).revokeAll(anyLong());
    }

    @Test
    void revokesAndInvalidatesOnlyAfterCommit() {
        prepare(0, 0);
        AdminUserUpdateReq request = request();
        request.setUserStatus(0);
        TransactionSynchronizationManager.initSynchronization();
        assertTrue(service.updateAdminUser(request).getData());
        verify(sessions, never()).revokeAll(anyLong());
        verifyNoInteractions(cache);
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
        verify(sessions).revokeAll(22L);
        verify(cache).del(CommonConst.USER_CACHE + 22L);
    }

    @ParameterizedTest
    @CsvSource({"0,0,true", "0,1,true", "0,2,true", "1,0,false", "1,1,true", "1,2,true",
            "2,0,false", "2,1,false", "2,2,false"})
    void deletionUsesSameRoleMatrixAndLogicalDelete(int operatorRole, int targetRole, boolean allowed) {
        prepare(operatorRole, targetRole);
        assertTrue(TableInfoHelper.getTableInfo(User.class).isWithLogicDelete());
        if (allowed) {
            assertEquals(200, service.deleteAdminUser(22L).getCode());
            verify(users).deleteById(argThat((User deleted) -> deleted.getId().equals(22L)));
            verify(sessions).revokeAll(22L);
            verify(cache).del(CommonConst.USER_CACHE + 22L);
        } else {
            assertEquals(403, assertThrows(AuthAccessException.class, () -> service.deleteAdminUser(22L)).getStatus());
            verify(users, never()).deleteById(any(User.class));
            verify(sessions, never()).revokeAll(anyLong());
            verifyNoInteractions(cache);
        }
    }

    @Test
    void deletionRejectsMissingTargetInvalidIdAndConcurrentPromotion() {
        prepare(1, 2);
        assertThrows(BizException.class, () -> service.deleteAdminUser(null));
        assertThrows(BizException.class, () -> service.deleteAdminUser(0L));
        when(users.selectList(any())).thenReturn(List.of(user(11, 1)));
        assertThrows(BizException.class, () -> service.deleteAdminUser(22L));
        when(users.selectList(any())).thenReturn(List.of(user(11, 1), user(22, 0)));
        assertThrows(AuthAccessException.class, () -> service.deleteAdminUser(22L));
        verify(users, never()).deleteById(any(User.class));
    }

    @Test
    void failedDeleteDoesNotRevokeSessions() {
        prepare(0, 2);
        when(users.deleteById(any(User.class))).thenReturn(0);
        assertThrows(BizException.class, () -> service.deleteAdminUser(22L));
        verify(sessions, never()).revokeAll(anyLong());
        verifyNoInteractions(cache);
    }

    @Test
    void deletionRevokesOnlyAfterCommit() {
        prepare(0, 2);
        TransactionSynchronizationManager.initSynchronization();
        service.deleteAdminUser(22L);
        verify(sessions, never()).revokeAll(anyLong());
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
        verify(sessions).revokeAll(22L);
    }

    @Test
    void legacyMutationEndpointsCannotBypassBossProtection() {
        prepare(1, 0);
        AdminUserController controller = new AdminUserController();
        ReflectionTestUtils.setField(controller, "userService", service);
        assertThrows(AuthAccessException.class, () -> controller.changeUserStatus(22L, false));
        assertThrows(AuthAccessException.class, () -> controller.changeUserAdmire(22L, "赞赏"));
        assertThrows(AuthAccessException.class, () -> controller.changeUserType(22L, 2));
        verify(users, never()).updateById(any(User.class));
    }
}
