package io.charqe.qs.modules.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.exception.handle.GlobalExceptionHandler;
import io.charqe.qs.modules.file.controller.admin.FileGovernanceController;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileUnreferencedReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.service.FileGovernanceService;
import io.charqe.qs.modules.user.auth.SessionScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 治理端点的 HTTP 契约：只读扫描、逐条结果、{@code {list,total,page,pageSize}} 口径与管理员作用域。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FileGovernanceHttpContractTest {

    private FileGovernanceService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(FileGovernanceService.class);
        var controller = new FileGovernanceController();
        ReflectionTestUtils.setField(controller, "fileGovernanceService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void scanReturnsOrphanEnvelopeAndForwardsLimit() throws Exception {
        var item = new FileOrphanScanRespVO.Item();
        item.setKey("avatar/3/202610/abc.png");
        item.setSize(128L);
        item.setMimeType("image/png");
        var resp = new FileOrphanScanRespVO();
        resp.setDryRun(true);
        resp.setStoreType("qiniu");
        resp.setPrefix("avatar/");
        resp.setScanned(2);
        resp.setRegistered(1);
        resp.setOrphanCount(1);
        resp.setTruncated(false);
        resp.setList(List.of(item));
        when(service.scan(any())).thenReturn(resp);

        mvc.perform(post("/file/orphan/scan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prefix\":\"avatar/\",\"limit\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.scanned").value(2))
                .andExpect(jsonPath("$.data.registered").value(1))
                .andExpect(jsonPath("$.data.orphanCount").value(1))
                .andExpect(jsonPath("$.data.truncated").value(false))
                .andExpect(jsonPath("$.data.list[0].key").value("avatar/3/202610/abc.png"));

        ArgumentCaptor<FileOrphanScanReqVO> captor = ArgumentCaptor.forClass(FileOrphanScanReqVO.class);
        verify(service).scan(captor.capture());
        assertEquals("avatar/", captor.getValue().getPrefix());
        assertEquals(Integer.valueOf(2), captor.getValue().getLimit());
    }

    @Test
    void scanUsesRequestDefaultsForEmptyObject() throws Exception {
        when(service.scan(any())).thenReturn(new FileOrphanScanRespVO());

        mvc.perform(post("/file/orphan/scan").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<FileOrphanScanReqVO> captor = ArgumentCaptor.forClass(FileOrphanScanReqVO.class);
        verify(service).scan(captor.capture());
        assertEquals(Integer.valueOf(1000), captor.getValue().getLimit());
        assertEquals(Boolean.TRUE, captor.getValue().getDryRun());
    }

    @Test
    void backfillRejectsEmptyKeys() throws Exception {
        mvc.perform(post("/file/orphan/backfill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keys\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(service, never()).backfill(any());
    }

    @Test
    void backfillRejectsMoreThanHundredKeys() throws Exception {
        StringBuilder body = new StringBuilder("{\"keys\":[");
        for (int i = 0; i < 101; i++) {
            body.append(i == 0 ? "" : ",").append("\"k").append(i).append(".png\"");
        }
        body.append("]}");

        mvc.perform(post("/file/orphan/backfill").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(service, never()).backfill(any());
    }

    @Test
    void backfillForwardsDryRunFlag() throws Exception {
        var item = new FileOrphanBackfillRespVO.Item();
        item.setKey("avatar/a.png");
        item.setOk(true);
        item.setSkipped(false);
        item.setFileId(11L);
        var resp = new FileOrphanBackfillRespVO();
        resp.setDryRun(false);
        resp.setTotal(1);
        resp.setRegistered(1);
        resp.setSkipped(0);
        resp.setList(List.of(item));
        when(service.backfill(any())).thenReturn(resp);

        mvc.perform(post("/file/orphan/backfill").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keys\":[\"avatar/a.png\"],\"dryRun\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dryRun").value(false))
                .andExpect(jsonPath("$.data.registered").value(1))
                .andExpect(jsonPath("$.data.list[0].fileId").value(11));

        ArgumentCaptor<FileOrphanBackfillReqVO> captor = ArgumentCaptor.forClass(FileOrphanBackfillReqVO.class);
        verify(service).backfill(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getDryRun());
        assertEquals(List.of("avatar/a.png"), captor.getValue().getKeys());
    }

    @Test
    void unreferencedListReturnsStandardEnvelope() throws Exception {
        var file = new FileInfo();
        file.setId(12L);
        file.setOriginalName("old.png");
        var page = new Page<FileInfo>(2, 5);
        page.setTotal(7);
        page.setRecords(List.of(file));
        when(service.pageUnreferenced(any())).thenReturn(page);

        mvc.perform(post("/file/unreferenced/list").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageNum\":2,\"pageSize\":5,\"days\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.list[0].id").value(12))
                .andExpect(jsonPath("$.data.total").value(7))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.days").value(30))
                .andExpect(jsonPath("$.data.records").doesNotExist());
    }

    @Test
    void unreferencedListDefaultsToSevenDays() throws Exception {
        when(service.pageUnreferenced(any())).thenReturn(new Page<FileInfo>(1, 10));

        mvc.perform(post("/file/unreferenced/list").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.days").value(7));

        ArgumentCaptor<FileUnreferencedReqVO> captor = ArgumentCaptor.forClass(FileUnreferencedReqVO.class);
        verify(service).pageUnreferenced(captor.capture());
        assertEquals(Integer.valueOf(7), captor.getValue().getDays());
    }

    @Test
    void batchRejectsEmptyIds() throws Exception {
        mvc.perform(post("/file/batch").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(service, never()).batch(any());
    }

    @Test
    void batchRejectsMoreThanHundredIds() throws Exception {
        StringBuilder body = new StringBuilder("{\"ids\":[");
        for (int i = 1; i <= 101; i++) {
            body.append(i == 1 ? "" : ",").append(i);
        }
        body.append("]}");

        mvc.perform(post("/file/batch").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(service, never()).batch(any());
    }

    @Test
    void batchReturnsPerItemResultAndDefaultsToDryRun() throws Exception {
        var ok = new FileBatchRespVO.Item();
        ok.setId(7L);
        ok.setOk(true);
        ok.setSkipped(false);
        ok.setAction("soft-delete");
        var skipped = new FileBatchRespVO.Item();
        skipped.setId(8L);
        skipped.setOk(false);
        skipped.setSkipped(true);
        skipped.setReason("文件仍被 2 处引用");
        var resp = new FileBatchRespVO();
        resp.setDryRun(true);
        resp.setForce(false);
        resp.setTotal(2);
        resp.setDeleted(1);
        resp.setSkipped(1);
        resp.setList(List.of(ok, skipped));
        when(service.batch(any())).thenReturn(resp);

        mvc.perform(post("/file/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[7,8]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.dryRun").value(true))
                .andExpect(jsonPath("$.data.deleted").value(1))
                .andExpect(jsonPath("$.data.skipped").value(1))
                .andExpect(jsonPath("$.data.list[0].action").value("soft-delete"))
                .andExpect(jsonPath("$.data.list[1].reason").value("文件仍被 2 处引用"));

        ArgumentCaptor<FileBatchReqVO> captor = ArgumentCaptor.forClass(FileBatchReqVO.class);
        verify(service).batch(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().getDryRun());
        assertEquals(Boolean.FALSE, captor.getValue().getForce());
    }

    @Test
    void governanceEndpointsRequireAdminSession() throws Exception {
        List<java.lang.reflect.Method> methods = List.of(
                FileGovernanceController.class.getMethod("scan", FileOrphanScanReqVO.class),
                FileGovernanceController.class.getMethod("backfill", FileOrphanBackfillReqVO.class),
                FileGovernanceController.class.getMethod("listUnreferenced", FileUnreferencedReqVO.class),
                FileGovernanceController.class.getMethod("batch", FileBatchReqVO.class));
        for (var method : methods) {
            var login = method.getAnnotation(LoginCheck.class);
            assertEquals(1, login.value());
            assertEquals(SessionScope.ADMIN, login.scope());
        }
    }

    @Test
    void batchWithEmptyIdListIsReportedWithValidationMessage() throws Exception {
        mvc.perform(post("/file/batch").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[],\"dryRun\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("文件ID不能为空")));

        verify(service, never()).batch(any());
    }
}
