package io.charqe.qs.modules.file.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.web.FileController;
import io.charqe.qs.modules.file.domain.dao.FileBizRelationMapper;
import io.charqe.qs.modules.file.domain.pojo.FileBizRef;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileBizRefServiceImpl;
import io.charqe.qs.modules.file.service.FileInfoService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件引用绑定/解绑/查询的 HTTP 契约：只替换持久化与文件查询，业务校验与分发走真实实现。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FileRefHttpContractTest {

    private static final long FILE_ID = 9L;
    private static final long USER_ID = 31L;

    private FileBizRefServiceImpl service;
    private FileBizRelationMapper mapper;
    private FileInfoService fileInfoService;
    private MockMvc mvc;
    private FileBizRef saved;
    private User user;

    @BeforeEach
    void setup() {
        service = spy(new FileBizRefServiceImpl());
        mapper = mock(FileBizRelationMapper.class);
        fileInfoService = mock(FileInfoService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "fileInfoService", fileInfoService);
        doReturn(null).when(service).getOne(any(Wrapper.class), anyBoolean());
        doAnswer(call -> {
            saved = call.getArgument(0);
            saved.setId(41L);
            return true;
        }).when(service).save(any(FileBizRef.class));

        user = new User();
        user.setId(USER_ID);
        user.setUserType(2);
        var sessions = mock(AuthSessionService.class);
        when(sessions.current(any(SessionScope.class), anyBoolean()))
                .thenReturn(new AuthSessionService.Resolved(null, user, "test-token"));

        var controller = new FileController();
        ReflectionTestUtils.setField(controller, "fileBizRefService", service);
        ReflectionTestUtils.setField(controller, "sessions", sessions);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private void fileOwnedBy(long ownerId) {
        var file = new FileInfo();
        file.setId(FILE_ID);
        file.setOwnerId(ownerId);
        file.setScene("moment");
        doReturn(file).when(fileInfoService).getById(FILE_ID);
    }

    @Test
    void clientBindsOwnFileAndIncrementsRefCount() throws Exception {
        fileOwnedBy(USER_ID);

        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(41))
                .andExpect(jsonPath("$.data.fileId").value(9))
                .andExpect(jsonPath("$.data.bizType").value(2))
                .andExpect(jsonPath("$.data.bizId").value(77))
                .andExpect(jsonPath("$.data.bizField").value(""));

        assertEquals("", saved.getBizField());
        assertEquals(USER_ID + "", saved.getCreateBy());
        assertEquals(Integer.valueOf(1), saved.getStatus());
        assertEquals(Integer.valueOf(0), saved.getSort());
        verify(fileInfoService).increaseRefCount(FILE_ID);
    }

    @Test
    void bindingSameReferenceTwiceIsIdempotent() throws Exception {
        fileOwnedBy(USER_ID);
        var existing = new FileBizRef();
        existing.setId(41L);
        existing.setFileId(FILE_ID);
        existing.setBizType(2);
        existing.setBizId(77L);
        existing.setBizField("");
        doReturn(existing).when(service).getOne(any(Wrapper.class), anyBoolean());

        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(41));

        verify(service, never()).save(any(FileBizRef.class));
        verify(fileInfoService, never()).increaseRefCount(any());
    }

    @Test
    void bindRejectsUnknownBizTypeBeforeLoadingFile() throws Exception {
        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":99,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("未知的业务类型：99"));

        verify(fileInfoService, never()).getById(any());
        verify(service, never()).save(any(FileBizRef.class));
    }

    @Test
    void bindRejectsForeignFile() throws Exception {
        fileOwnedBy(99L);

        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("无权操作该文件"));

        verify(service, never()).save(any(FileBizRef.class));
        verify(fileInfoService, never()).increaseRefCount(any());
    }

    @Test
    void bindRejectsDeletedFile() throws Exception {
        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件不存在或已删除"));
    }

    @Test
    void malformedBizFieldIsRejectedByValidation() throws Exception {
        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77,\"bizField\":\"bad field\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(service, never()).save(any(FileBizRef.class));
    }

    @Test
    void managerCanBindAnyFile() throws Exception {
        user.setUserType(0);
        fileOwnedBy(99L);

        mvc.perform(post("/file/ref/bind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":1,\"bizId\":5,\"bizField\":\"cover\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bizField").value("cover"));

        verify(fileInfoService).increaseRefCount(FILE_ID);
    }

    @Test
    void unbindDeletesReferenceAndDecrementsRefCount() throws Exception {
        fileOwnedBy(USER_ID);
        var existing = new FileBizRef();
        existing.setId(41L);
        existing.setFileId(FILE_ID);
        doReturn(existing).when(service).getOne(any(Wrapper.class), anyBoolean());

        mvc.perform(post("/file/ref/unbind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(mapper).purgeById(41L);
        verify(fileInfoService).decreaseRefCount(FILE_ID);
    }

    @Test
    void unbindMissingReferenceIsIdempotent() throws Exception {
        fileOwnedBy(USER_ID);

        mvc.perform(post("/file/ref/unbind").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9,\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(false));

        verify(mapper, never()).purgeById(anyLong());
        verify(fileInfoService, never()).decreaseRefCount(any());
    }

    @Test
    void listByFileIdReturnsStandardEnvelope() throws Exception {
        fileOwnedBy(USER_ID);
        var ref = new FileBizRef();
        ref.setId(41L);
        ref.setFileId(FILE_ID);
        ref.setBizType(2);
        ref.setBizId(77L);
        ref.setBizField("cover");
        var page = new Page<FileBizRef>(1, 20);
        page.setTotal(3);
        page.setRecords(java.util.List.of(ref));
        doReturn(page).when(service).page(any(IPage.class), any(Wrapper.class));

        mvc.perform(post("/file/ref/list").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].bizField").value("cover"))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.records").doesNotExist());
    }

    @Test
    void listByBizDimensionRequiresManager() throws Exception {
        mvc.perform(post("/file/ref/list").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizType\":2,\"bizId\":77}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("仅管理员可按业务维度查询引用"));

        verify(service, never()).page(any(IPage.class), any(Wrapper.class));
    }

    @Test
    void listWithoutAnyConditionIsRejected() throws Exception {
        mvc.perform(post("/file/ref/list").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("请提供文件ID或业务类型+业务ID"));
    }

    @Test
    void refEndpointsAllowEitherSessionScope() throws Exception {
        for (var method : new java.lang.reflect.Method[]{
                FileController.class.getMethod("bindRef", io.charqe.qs.modules.file.controller.admin.vo.FileRefBindReqVO.class),
                FileController.class.getMethod("unbindRef", io.charqe.qs.modules.file.controller.admin.vo.FileRefUnbindReqVO.class),
                FileController.class.getMethod("listRefs", io.charqe.qs.modules.file.controller.admin.vo.FileRefListReqVO.class)}) {
            var login = method.getAnnotation(LoginCheck.class);
            assertEquals(2, login.value());
            assertEquals(SessionScope.EITHER, login.scope());
        }
    }
}
