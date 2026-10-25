package io.charqe.qs.modules.file.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.FileVO;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.admin.FileAdminController;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileInfoServiceImpl;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.modules.user.domain.pojo.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 实际 MVC 校验、服务层派生与 MapStruct 转换；只替换持久化操作，不连接数据库或 Redis。 */
class FileAdminHttpContractTest {
    private final ObjectMapper json = new ObjectMapper();
    private FileInfoServiceImpl service;
    private MockMvc mvc;
    private FileInfo saved;

    @BeforeEach
    void setup() {
        service = spy(new FileInfoServiceImpl());
        doAnswer(call -> {
            saved = call.getArgument(0);
            saved.setId(8L);
            return true;
        }).when(service).save(any(FileInfo.class));
        var user = new User();
        user.setId(22L);
        user.setUserType(0);
        var sessions = mock(AuthSessionService.class);
        when(sessions.current(any(SessionScope.class), anyBoolean()))
                .thenReturn(new AuthSessionService.Resolved(null, user, "test-token"));

        var controller = new FileAdminController();
        ReflectionTestUtils.setField(controller, "fileInfoService", service);
        ReflectionTestUtils.setField(controller, "sessions", sessions);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private FileCreateReqVO request() {
        var request = new FileCreateReqVO();
        request.setOriginalName("文章封面.PNG");
        request.setStoragePath("cover/user22-123.PNG");
        request.setMimeType("image/png");
        request.setFileSize(1024L);
        request.setThirdPartyName(3);
        request.setCdnDomain("https://cdn.example.invalid/assets/");
        return request;
    }

    @Test
    void savesAndRespondsWithEntityFieldNames() throws Exception {
        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(8))
                .andExpect(jsonPath("$.data.originalName").value("文章封面.PNG"))
                .andExpect(jsonPath("$.data.storagePath").value("cover/user22-123.PNG"))
                .andExpect(jsonPath("$.data.mimeType").value("image/png"))
                .andExpect(jsonPath("$.data.thirdPartyName").value(3))
                .andExpect(jsonPath("$.data.fileSize").value(1024))
                .andExpect(jsonPath("$.data.cdnDomain").value("https://cdn.example.invalid/assets/"))
                .andExpect(jsonPath("$.data.storedName").value("user22-123.PNG"))
                .andExpect(jsonPath("$.data.fileCategory").value("image"))
                .andExpect(jsonPath("$.data.fileExt").value("png"))
                .andExpect(jsonPath("$.data.scene").value("unknown"))
                .andExpect(jsonPath("$.data.fileName").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.fileType").doesNotExist())
                .andExpect(jsonPath("$.data.storageType").doesNotExist())
                .andExpect(jsonPath("$.data.accessUrl").doesNotExist());
        assertNull(saved.getFileHash());
        assertEquals("", saved.getBucket());
        assertEquals(22L, saved.getOwnerId());
        assertEquals("admin", saved.getOwnerType());
        assertEquals("unknown", saved.getScene());
        assertEquals(Integer.valueOf(1), saved.getStatus());
    }

    @ParameterizedTest
    @CsvSource({"video/mp4,video", "audio/mpeg,audio", "text/plain,document", "application/pdf,document", "application/zip,other"})
    void derivesCategoryInService(String mime, String category) {
        var request = request();
        request.setMimeType(mime);
        var response = service.save(request);
        assertEquals(category, saved.getFileCategory());
        assertEquals(category, response.getFileCategory());
    }

    @Test
    void derivesEmptyExtensionWhenStorageNameHasNoSuffix() {
        var request = request();
        request.setStoragePath("/files/README");
        var response = service.save(request);
        assertEquals("README", response.getStoredName());
        assertEquals("", response.getFileExt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/", "/assets/", "https://cdn.example.invalid", "https://cdn.example.invalid/assets/"})
    void acceptsStableCdnPrefixes(String domain) throws Exception {
        var request = request();
        request.setCdnDomain(domain);
        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        assertEquals(domain, saved.getCdnDomain());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "https://cdn.example.invalid/?signature=temporary",
            "https://cdn.example.invalid/#fragment", "https://user@cdn.example.invalid/",
            "//cdn.example.invalid/", "https:///missing-host/", "https://cdn.example.invalid/bad path"})
    void invalidCdnReturnsHttp400BeforePersistence(String domain) throws Exception {
        var request = request();
        request.setCdnDomain(domain);
        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verify(service, never()).save(any(FileCreateReqVO.class));
        assertNull(saved);
    }

    @Test
    void legacyNamesDoNotSilentlyCreateIncompleteRecords() throws Exception {
        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON).content("""
                {"fileName":"old.png","filePath":"cover/old.png","fileType":"image/png",
                 "storageType":3,"accessUrl":"https://cdn.example.invalid/cover/old.png"}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verify(service, never()).save(any(FileCreateReqVO.class));
    }

    @Test
    void requestCannotAssignReadOnlyOrInternalFields() throws Exception {
        var body = json.valueToTree(request());
        var fields = (com.fasterxml.jackson.databind.node.ObjectNode) body;
        fields.put("id", 999).put("ownerId", 999).put("status", 1).put("bucket", "forged")
                .put("storedName", "forged").put("fileExt", "exe")
                .put("fileCategory", "forged");
        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storedName").value("user22-123.PNG"));
        assertEquals("png", saved.getFileExt());
        assertEquals("image", saved.getFileCategory());
        assertEquals(22L, saved.getOwnerId());
        assertEquals("", saved.getBucket());
        assertNull(saved.getFileHash());
        assertEquals(Integer.valueOf(1), saved.getStatus());
    }

    @Test
    void fileHashIsStoredLowercased() {
        var request = request();
        request.setFileHash("ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789");

        service.save(request);

        assertEquals("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                saved.getFileHash());
    }

    @Test
    void malformedFileHashIsRejected() throws Exception {
        var body = json.valueToTree(request());
        ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("fileHash", "forged");

        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        assertNull(saved);
    }

    @Test
    void saveAndUploadDeclareAdminScope() throws Exception {
        for (var method : new java.lang.reflect.Method[]{
                FileAdminController.class.getMethod("save", FileCreateReqVO.class),
                FileAdminController.class.getMethod("upload", MultipartFile.class, FileVO.class)}) {
            var login = method.getAnnotation(LoginCheck.class);
            assertEquals(1, login.value());
            assertEquals(SessionScope.ADMIN, login.scope());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void listReturnsListTotalPagePageSize() throws Exception {
        var file = new FileInfo();
        file.setId(5L);
        file.setScene("avatar");
        file.setOriginalName("a.png");
        var page = new Page<FileInfo>(2, 10);
        page.setTotal(21);
        page.setRecords(java.util.List.of(file));
        doReturn(page).when(service).page(any(IPage.class), any(Wrapper.class));

        mvc.perform(post("/file/admin/list").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":2,\"pageSize\":10,\"scene\":\"avatar\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(5))
                .andExpect(jsonPath("$.data.total").value(21))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.records").doesNotExist());
    }

    @Test
    void deleteAcceptsJsonBodyId() throws Exception {
        doReturn(true).when(service).removeById(7L);

        mvc.perform(post("/file/admin/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(service).removeById(7L);
    }

    @Test
    void deleteWithoutIdIsRejected() throws Exception {
        mvc.perform(post("/file/admin/delete").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件ID不能为空"));
        verify(service, never()).removeById(any(java.io.Serializable.class));
    }
}
