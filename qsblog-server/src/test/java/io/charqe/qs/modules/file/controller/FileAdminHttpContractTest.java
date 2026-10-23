package io.charqe.qs.modules.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.FileVO;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.admin.FileAdminController;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileInfoServiceImpl;
import io.charqe.qs.modules.user.auth.SessionScope;
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
        var controller = new FileAdminController();
        ReflectionTestUtils.setField(controller, "fileInfoService", service);
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
                .andExpect(jsonPath("$.data.fileName").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.fileType").doesNotExist())
                .andExpect(jsonPath("$.data.storageType").doesNotExist())
                .andExpect(jsonPath("$.data.accessUrl").doesNotExist());
        assertNull(saved.getFileHash());
        assertNull(saved.getBucket());
        assertNull(saved.getOwnerId());
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
                .put("fileHash", "forged").put("storedName", "forged").put("fileExt", "exe")
                .put("fileCategory", "forged");
        mvc.perform(post("/file/admin/save").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storedName").value("user22-123.PNG"));
        assertEquals("png", saved.getFileExt());
        assertEquals("image", saved.getFileCategory());
        assertNull(saved.getOwnerId());
        assertNull(saved.getBucket());
        assertNull(saved.getFileHash());
        assertNull(saved.getStatus());
    }

    @Test
    void saveAndUploadStillDeclareOwnerOnlyAdminScope() throws Exception {
        for (var method : new java.lang.reflect.Method[]{
                FileAdminController.class.getMethod("save", FileCreateReqVO.class),
                FileAdminController.class.getMethod("upload", MultipartFile.class, FileVO.class)}) {
            var login = method.getAnnotation(LoginCheck.class);
            assertEquals(0, login.value());
            assertEquals(SessionScope.ADMIN, login.scope());
        }
    }
}
