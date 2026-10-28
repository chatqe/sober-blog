package io.charqe.qs.modules.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.infra.storage.QiniuUtil;
import io.charqe.qs.modules.file.controller.web.FileController;
import io.charqe.qs.modules.file.service.FileInfoService;
import io.charqe.qs.modules.file.service.FileUploadServiceImpl;
import io.charqe.qs.modules.file.support.FileKeyGenerator;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 上传凭证端点：服务端 Key、场景白名单、大小与类型限制、七牛策略。 */
class FileUploadHttpContractTest {

    private static final String KEY_PATTERN = "^[a-z-]+/\\d+/\\d{6}/[0-9A-HJKMNP-TV-Z]{26}\\.png$";

    private final ObjectMapper json = new ObjectMapper();

    private FileUploadServiceImpl uploadService;

    private AuthSessionService sessions;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        uploadService = new FileUploadServiceImpl();
        ReflectionTestUtils.setField(uploadService, "keyGenerator", new FileKeyGenerator());
        ReflectionTestUtils.setField(uploadService, "qiniuProvider", provider(null));
        ReflectionTestUtils.setField(uploadService, "storeType", "local");
        ReflectionTestUtils.setField(uploadService, "qiniuEnabled", false);
        ReflectionTestUtils.setField(uploadService, "qiniuUploadUrl", "");
        ReflectionTestUtils.setField(uploadService, "qiniuDownloadUrl", "https://cdn.example.invalid");
        ReflectionTestUtils.setField(uploadService, "localUploadUrl", "/upload");
        ReflectionTestUtils.setField(uploadService, "localDownloadUrl", "");
        ReflectionTestUtils.setField(uploadService, "fsizeLimit", 20971520L);
        ReflectionTestUtils.setField(uploadService, "tokenTtl", 3600L);

        sessions = mock(AuthSessionService.class);
        var controller = new FileController();
        ReflectionTestUtils.setField(controller, "fileUploadService", uploadService);
        ReflectionTestUtils.setField(controller, "fileInfoService", mock(FileInfoService.class));
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

    private String body(String scene, String mime, long size) {
        return "{\"scene\":\"" + scene + "\",\"originalName\":\"头像.PNG\",\"mimeType\":\""
                + mime + "\",\"fileSize\":" + size + "}";
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<QiniuUtil> provider(QiniuUtil util) {
        ObjectProvider<QiniuUtil> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(util);
        return provider;
    }

    private QiniuUtil qiniu() {
        var util = new QiniuUtil();
        ReflectionTestUtils.setField(util, "accessKey", "test-ak");
        ReflectionTestUtils.setField(util, "secretKey", "test-sk");
        ReflectionTestUtils.setField(util, "bucket", "test-bucket");
        return util;
    }

    @Test
    void clientGetsServerKeyForAvatar() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("avatar", "image/png", 2048)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.storeType").value("local"))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.key").value(matchesPattern(KEY_PATTERN)))
                .andExpect(jsonPath("$.data.key").value(matchesPattern("^avatar/31/.*")))
                .andExpect(jsonPath("$.data.insertOnly").value(1))
                .andExpect(jsonPath("$.data.mimeLimit").value("image/png"))
                .andExpect(jsonPath("$.data.hashAlgo").value("sha256"))
                .andExpect(jsonPath("$.data.fsizeLimit").value(20971520));
    }

    @Test
    void clientCannotRequestEditorialScene() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("article-cover", "image/png", 2048)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该场景不允许当前账号上传"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void managerGetsKeyForEditorialScene() throws Exception {
        loginAs(22L, 0);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("article-cover", "image/png", 2048)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.key").value(matchesPattern("^article-cover/22/.*")));
    }

    @Test
    void unknownSceneIsRejected() throws Exception {
        loginAs(22L, 0);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("self-made", "image/png", 2048)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("不支持的业务场景：self-made"));
    }

    @Test
    void oversizedFileIsRejected() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("avatar", "image/png", 20971521L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件大小超过 20MB 限制"));
    }

    @Test
    void disallowedMimeIsRejected() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("avatar", "text/html", 1024)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("不支持的文件类型：text/html"));
    }

    @Test
    void missingMetadataIsRejectedByValidation() throws Exception {
        loginAs(31L, 2);

        mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scene\":\"avatar\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void qiniuTokenCarriesInsertOnlyAndMimeLimit() throws Exception {
        ReflectionTestUtils.setField(uploadService, "storeType", "qiniu");
        ReflectionTestUtils.setField(uploadService, "qiniuEnabled", true);
        ReflectionTestUtils.setField(uploadService, "qiniuProvider", provider(qiniu()));
        loginAs(31L, 2);

        var response = mvc.perform(post("/file/uploadToken").contentType(MediaType.APPLICATION_JSON)
                        .content(body("avatar", "image/png", 2048)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.storeType").value("qiniu"))
                .andExpect(jsonPath("$.data.downloadUrl").value("https://cdn.example.invalid/"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String token = json.readTree(response).path("data").path("token").asText();
        String policy = new String(Base64.getUrlDecoder().decode(token.split(":")[2]), StandardCharsets.UTF_8);
        var policyJson = json.readTree(policy);
        assertEquals(1, policyJson.path("insertOnly").asInt());
        assertEquals("image/png", policyJson.path("mimeLimit").asText());
        assertEquals(20971520L, policyJson.path("fsizeLimit").asLong());
        assertTrue(policyJson.path("returnBody").asText().contains("$(etag)"));
    }

    @Test
    void configExposesScenesAndLimits() throws Exception {
        mvc.perform(get("/file/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.storeType").value("local"))
                .andExpect(jsonPath("$.data.clientScenes", contains("avatar", "moment", "comment")))
                .andExpect(jsonPath("$.data.fsizeLimit").value(20971520))
                .andExpect(jsonPath("$.data.hashAlgo").value("sha256"))
                .andExpect(jsonPath("$.data.tokenTtl").value(3600));
    }
}
