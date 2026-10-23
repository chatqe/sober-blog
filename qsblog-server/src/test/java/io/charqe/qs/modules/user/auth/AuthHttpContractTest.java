package io.charqe.qs.modules.user.auth;

import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.aop.LoginCheckAspect;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.user.controller.admin.AdminUserController;
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
    void ownerOnlyAspectRejectsManagerWithoutRenewing() throws Throwable {
        var aspect = new LoginCheckAspect();
        ReflectionTestUtils.setField(aspect, "sessions", sessions);
        var metadata = new SaTokenSessionAdapter.Metadata();
        metadata.setSessionType(SessionScope.ADMIN);
        when(sessions.current(SessionScope.ADMIN, false)).thenReturn(
                new AuthSessionService.Resolved(metadata, AuthSessionServiceTest.user(2, 1), "admin"));
        LoginCheck annotation = AdminUserController.class.getMethod("listUser",
                io.charqe.qs.common.model.BaseReqVO.class).getAnnotation(LoginCheck.class);
        var point = mock(ProceedingJoinPoint.class);
        assertEquals(403, assertThrows(AuthAccessException.class, () -> aspect.around(point, annotation)).getStatus());
        verify(sessions, never()).current(SessionScope.ADMIN, true);
        verifyNoInteractions(point);
    }
}
