package io.charqe.qs.modules.user.auth;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.LoginCheckAspect;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.user.controller.admin.AdminUserController;
import io.charqe.qs.modules.user.controller.admin.vo.AdminUserUpdateReq;
import io.charqe.qs.modules.user.controller.web.UserController;
import io.charqe.qs.modules.user.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthHttpContractTest {
    UserService users;
    AuthSessionService sessions;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        users = mock(UserService.class);
        sessions = mock(AuthSessionService.class);
        var web = new UserController();
        var admin = new AdminUserController();
        for (Object controller : new Object[]{web, admin}) {
            ReflectionTestUtils.setField(controller, "userService", users);
            ReflectionTestUtils.setField(controller, "sessions", sessions);
        }
        mvc = MockMvcBuilders.standaloneSetup(web, admin)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void loginEndpointsSelectFixedDomain() throws Exception {
        when(users.login(anyString(), anyString(), any())).thenReturn(R.success(new AuthSessionVO()));
        mvc.perform(post("/user/login").param("account", "a").param("password", "b"))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/user/login").param("account", "c").param("password", "d"))
                .andExpect(status().isOk());
        mvc.perform(post("/user/login").param("account", "a").param("password", "b").param("isAdmin", "true"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verify(users).login("a", "b", SessionScope.WEB);
        verify(users).login("c", "d", SessionScope.ADMIN);
        verifyNoMoreInteractions(users);
    }

    @Test
    void errorsPreserveHttpAndBusinessStatus() throws Exception {
        when(sessions.inspect(SessionScope.WEB)).thenThrow(new AuthAccessException(401, "登录过期"));
        when(sessions.inspect(SessionScope.ADMIN)).thenThrow(new AuthAccessException(403, "会话类型不符"));
        mvc.perform(get("/user/session")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mvc.perform(get("/admin/user/session")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateAcceptsStringIdAndRejectsInvalidPayload() throws Exception {
        when(users.updateAdminUser(any())).thenReturn(R.success(false));
        mvc.perform(post("/admin/user/update").contentType("application/json")
                        .content("{\"id\":\"9007199254740993\",\"username\":\"新用户名\",\"userType\":1,\"userStatus\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(false));
        verify(users).updateAdminUser(argThat(request -> request.getId().equals(9007199254740993L)
                && request.getUserType() == 1 && request.getUserStatus() == 1));
        clearInvocations(users);
        for (String payload : new String[]{"{\"username\":\"用户\"}", "{\"id\":\"22\",\"userType\":-1}",
                "{\"id\":\"22\",\"userStatus\":2}", "{\"id\":\"22\",\"gender\":3}"}) {
            mvc.perform(post("/admin/user/update").contentType("application/json").content(payload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(users);
    }

    @Test
    void updateAcceptsContactsAndEmptyValuesButRejectsMalformedContacts() throws Exception {
        when(users.updateAdminUser(any())).thenReturn(R.success(true));
        mvc.perform(post("/admin/user/update").contentType("application/json")
                        .content("{\"id\":\"22\",\"phoneNumber\":\" 13800138000 \",\"email\":\" user@example.com \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(users).updateAdminUser(argThat(request -> "13800138000".equals(request.getPhoneNumber())
                && "user@example.com".equals(request.getEmail())));
        mvc.perform(post("/admin/user/update").contentType("application/json")
                        .content("{\"id\":\"22\",\"phoneNumber\":\"\",\"email\":\"\"}"))
                .andExpect(status().isOk());
        verify(users).updateAdminUser(argThat(request -> "".equals(request.getPhoneNumber())
                && "".equals(request.getEmail())));
        clearInvocations(users);
        for (String payload : new String[]{"{\"id\":\"22\",\"phoneNumber\":\"123\"}",
                "{\"id\":\"22\",\"email\":\"invalid\"}", "{\"id\":\"22\",\"email\":\"user@localhost\"}",
                "{\"id\":\"22\",\"email\":\"123456789012345678901@example.comx\"}"}) {
            mvc.perform(post("/admin/user/update").contentType("application/json").content(payload))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(users);
    }

    @Test
    void deleteUsesStringIdAndPropagatesPermissionDenial() throws Exception {
        when(users.deleteAdminUser(9007199254740993L)).thenReturn(R.success());
        mvc.perform(delete("/admin/user/9007199254740993"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(users).deleteAdminUser(9007199254740993L);
        when(users.deleteAdminUser(22L)).thenThrow(new AuthAccessException(403, "不能操作 Boss"));
        mvc.perform(delete("/admin/user/22"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void updateRequiresAdminSessionAndAllowsManagerAtAspect() throws Throwable {
        var aspect = new LoginCheckAspect();
        ReflectionTestUtils.setField(aspect, "sessions", sessions);
        LoginCheck annotation = AdminUserController.class.getMethod("updateAdminUser", AdminUserUpdateReq.class)
                .getAnnotation(LoginCheck.class);
        assertEquals(SessionScope.ADMIN, annotation.scope());
        var metadata = new SaTokenSessionAdapter.Metadata();
        metadata.setSessionType(SessionScope.ADMIN);
        when(sessions.current(SessionScope.ADMIN, false)).thenReturn(
                new AuthSessionService.Resolved(metadata, AuthSessionServiceTest.user(11, 1), "admin"));
        var point = mock(ProceedingJoinPoint.class);
        aspect.around(point, annotation);
        verify(point).proceed();
        clearInvocations(point);
        when(sessions.current(SessionScope.ADMIN, false)).thenThrow(new AuthAccessException(403, "会话类型不符"));
        assertThrows(AuthAccessException.class, () -> aspect.around(point, annotation));
        verifyNoInteractions(point);
    }

    @Test
    void adminAspectRejectsNormalWithoutRenewing() throws Throwable {
        var aspect = new LoginCheckAspect();
        ReflectionTestUtils.setField(aspect, "sessions", sessions);
        var metadata = new SaTokenSessionAdapter.Metadata();
        metadata.setSessionType(SessionScope.ADMIN);
        when(sessions.current(SessionScope.ADMIN, false)).thenReturn(
                new AuthSessionService.Resolved(metadata, AuthSessionServiceTest.user(2, 2), "admin"));
        var point = mock(ProceedingJoinPoint.class);
        for (var method : AdminUserController.class.getDeclaredMethods()) {
            LoginCheck annotation = method.getAnnotation(LoginCheck.class);
            if (annotation == null) continue;
            assertEquals(SessionScope.ADMIN, annotation.scope());
            assertEquals(403, assertThrows(AuthAccessException.class, () -> aspect.around(point, annotation)).getStatus());
        }
        verify(sessions, never()).current(SessionScope.ADMIN, true);
        verifyNoInteractions(point);
    }
}
