package io.charqe.qs.modules.file.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.admin.FileRecycleController;
import io.charqe.qs.modules.file.controller.admin.vo.FileListReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileInfoServiceImpl;
import io.charqe.qs.modules.user.auth.SessionScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 回收站列表与恢复的 HTTP 契约：保持 {@code {list,total,page,pageSize}} 口径与管理员作用域。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FileRecycleHttpContractTest {

    private FileInfoServiceImpl service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = spy(new FileInfoServiceImpl());
        var controller = new FileRecycleController();
        ReflectionTestUtils.setField(controller, "fileInfoService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listReturnsListTotalPagePageSizeForDeletedFiles() throws Exception {
        var file = new FileInfo();
        file.setId(12L);
        file.setOriginalName("old.png");
        file.setDeletedAt(LocalDateTime.of(2026, 10, 26, 10, 30));
        var page = new Page<FileInfo>(2, 5);
        page.setTotal(7);
        page.setRecords(java.util.List.of(file));
        doReturn(page).when(service).pageDeleted(any(FileListReqVO.class));

        mvc.perform(post("/file/recycle/list").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":2,\"pageSize\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(12))
                .andExpect(jsonPath("$.data.list[0].deletedAt").exists())
                .andExpect(jsonPath("$.data.total").value(7))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.records").doesNotExist());
    }

    @Test
    void listAndRestoreDeclareAdminScope() throws Exception {
        for (var method : new java.lang.reflect.Method[]{
                FileRecycleController.class.getMethod("list", FileListReqVO.class),
                FileRecycleController.class.getMethod("restore", Long.class,
                        io.charqe.qs.modules.file.controller.admin.vo.FileDeleteReqVO.class)}) {
            var login = method.getAnnotation(LoginCheck.class);
            assertEquals(1, login.value());
            assertEquals(SessionScope.ADMIN, login.scope());
        }
    }

    @Test
    void restoreCallsServiceForExistingFile() throws Exception {
        var file = new FileInfo();
        file.setId(12L);
        file.setIsDel(1);
        doReturn(file).when(service).findAny(12L);
        doReturn(1).when(service).restore(12L);

        mvc.perform(post("/file/recycle/restore").contentType(MediaType.APPLICATION_JSON).content("{\"id\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(service).restore(12L);
    }

    @Test
    void queryIdTakesPrecedenceOverBody() throws Exception {
        var file = new FileInfo();
        file.setId(12L);
        doReturn(file).when(service).findAny(12L);
        doReturn(1).when(service).restore(12L);

        mvc.perform(post("/file/recycle/restore?id=12").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(service).restore(12L);
        verify(service, never()).restore(99L);
    }

    @Test
    void restoreUnknownFileReturns404() throws Exception {
        doReturn(null).when(service).findAny(12L);

        mvc.perform(post("/file/recycle/restore").contentType(MediaType.APPLICATION_JSON).content("{\"id\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value("文件不存在"));

        verify(service, never()).restore(any());
    }

    @Test
    void restoreWithoutIdIsRejected() throws Exception {
        mvc.perform(post("/file/recycle/restore").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("文件ID不能为空"));

        verify(service, never()).restore(any());
    }
}
