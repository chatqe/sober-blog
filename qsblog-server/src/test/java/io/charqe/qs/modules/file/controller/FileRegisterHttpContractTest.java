package io.charqe.qs.modules.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.web.FileController;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileInfoServiceImpl;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C 端统一登记端点：拥有者取自会话，场景白名单按账号类型收敛。 */
class FileRegisterHttpContractTest {

    private final ObjectMapper json = new ObjectMapper();
    private FileInfoServiceImpl service;
    private AuthSessionService sessions;
    private MockMvc mvc;
    private FileInfo saved;

    @BeforeEach
    void setup() {
        service = spy(new FileInfoServiceImpl());
        doAnswer(call -> {
            saved = call.getArgument(0);
            saved.setId(9L);
            return true;
        }).when(service).save(any(FileInfo.class));
        sessions = mock(AuthSessionService.class);

        var controller = new FileController();
        ReflectionTestUtils.setField(controller, "fileInfoService", service);
        ReflectionTestUtils.setField(controller, "sessions", sessions);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private void loginAs(Long id, Integer userType) {
        var user = new User();
        user.setId(id);
        user.setUserType(userType);
        when(sessions.current(any(SessionScope.class), anyBoolean()))
                .thenReturn(new AuthSessionService.Resolved(null, user, "test-token"));
    }

    private FileCreateReqVO request(String scene) {
        var request = new FileCreateReqVO();
        request.setOriginalName("头像.PNG");
        request.setStoragePath("avatar/user31-456.PNG");
        request.setMimeType("image/png");
        request.setFileSize(2048L);
        request.setThirdPartyName(3);
        request.setScene(scene);
        return request;
    }

    @Test
    void clientRegistersAvatarWithSessionOwner() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request("avatar"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.scene").value("avatar"))
                .andExpect(jsonPath("$.data.storedName").value("user31-456.PNG"));

        assertEquals(31L, saved.getOwnerId());
        assertEquals("user", saved.getOwnerType());
        assertEquals("avatar", saved.getScene());
        assertEquals(0, saved.getRefCount());
    }

    @Test
    void clientSceneOutsideWhitelistIsRejected() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request("article-cover"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该场景不允许当前账号上传"));

        assertNull(saved);
    }

    @Test
    void managerMayRegisterEditorialScene() throws Exception {
        loginAs(22L, 0);

        mvc.perform(post("/file/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request("article-cover"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.scene").value("article-cover"));

        assertEquals("admin", saved.getOwnerType());
        assertEquals("article-cover", saved.getScene());
    }

    @Test
    void missingSceneFallsBackToUnknown() throws Exception {
        loginAs(22L, 1);

        mvc.perform(post("/file/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.scene").value("unknown"));

        assertEquals("admin", saved.getOwnerType());
        assertEquals("unknown", saved.getScene());
    }

    @Test
    void registerDeclaresEitherScope() throws Exception {
        var login = FileController.class.getMethod("register", FileCreateReqVO.class).getAnnotation(LoginCheck.class);
        assertEquals(SessionScope.EITHER, login.scope());
    }
}
