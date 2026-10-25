package io.charqe.qs.modules.file.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.infra.storage.QiniuUtil;
import io.charqe.qs.infra.storage.StorageObject;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileBatchRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanBackfillRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileOrphanScanRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileUnreferencedReqVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 治理实现：扫描只读比对、补登记幂等/缺对象跳过、未引用过滤、批量 dryRun 与引用保护。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class FileGovernanceServiceTest {

    private FileGovernanceServiceImpl service;
    private FileInfoService fileInfoService;
    private QiniuUtil qiniu;

    @BeforeEach
    void setup() {
        // 纯单元测试不启动 MyBatis，手动注册 TableInfo 以便解析 Lambda 列名
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FileInfo.class);
        service = new FileGovernanceServiceImpl();
        fileInfoService = mock(FileInfoService.class);
        qiniu = mock(QiniuUtil.class);
        ObjectProvider<QiniuUtil> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(qiniu);
        ReflectionTestUtils.setField(service, "fileInfoService", fileInfoService);
        ReflectionTestUtils.setField(service, "qiniuProvider", provider);
        ReflectionTestUtils.setField(service, "storeType", "qiniu");
        ReflectionTestUtils.setField(service, "qiniuEnabled", true);
        ReflectionTestUtils.setField(service, "downloadUrl", "https://img.soberup.top/");
    }

    private StorageObject object(String key) {
        return new StorageObject(key, 128L, "image/png", 1L);
    }

    private FileInfo file(Long id, int refCount, int isDel) {
        FileInfo file = new FileInfo();
        file.setId(id);
        file.setRefCount(refCount);
        file.setIsDel(isDel);
        file.setStoragePath("avatar/" + id + ".png");
        return file;
    }

    @Test
    void scanSeparatesOrphansFromRegisteredAndFlagsTruncation() {
        when(qiniu.listObjects("avatar/", 3))
                .thenReturn(List.of(object("avatar/a.png"), object("avatar/b.png"), object("avatar/c.png")));
        when(fileInfoService.existingStoragePaths(anyCollection())).thenReturn(Set.of("avatar/b.png"));
        FileOrphanScanReqVO req = new FileOrphanScanReqVO();
        req.setPrefix("avatar/");
        req.setLimit(2);

        FileOrphanScanRespVO resp = service.scan(req);

        assertEquals(2, resp.getScanned().intValue());
        assertEquals(1, resp.getRegistered().intValue());
        assertEquals(1, resp.getOrphanCount().intValue());
        assertTrue(resp.getTruncated());
        assertEquals("avatar/a.png", resp.getList().get(0).getKey());
        assertEquals(128L, resp.getList().get(0).getSize().longValue());
        verify(qiniu).listObjects("avatar/", 3);
    }

    @Test
    void scanReportsAllRegisteredWhenNoOrphansRemain() {
        when(qiniu.listObjects("", 3)).thenReturn(List.of(object("a.png"), object("b.png")));
        when(fileInfoService.existingStoragePaths(anyCollection()))
                .thenReturn(Set.of("a.png", "b.png"));
        FileOrphanScanReqVO req = new FileOrphanScanReqVO();
        req.setLimit(2);

        FileOrphanScanRespVO resp = service.scan(req);

        assertFalse(resp.getTruncated());
        assertEquals(2, resp.getScanned().intValue());
        assertEquals(2, resp.getRegistered().intValue());
        assertEquals(0, resp.getOrphanCount().intValue());
        assertTrue(resp.getList().isEmpty());
    }

    @Test
    void scanSkipsDirectoryPlaceholderObjects() {
        when(qiniu.listObjects("qingsi/", 3))
                .thenReturn(List.of(object("qingsi/emoji/"), object("qingsi/emoji/q1.gif")));
        when(fileInfoService.existingStoragePaths(anyCollection())).thenReturn(Set.of());
        FileOrphanScanReqVO req = new FileOrphanScanReqVO();
        req.setPrefix("qingsi/");
        req.setLimit(2);

        FileOrphanScanRespVO resp = service.scan(req);

        assertEquals(1, resp.getScanned().intValue());
        assertEquals(1, resp.getOrphanCount().intValue());
        assertEquals("qingsi/emoji/q1.gif", resp.getList().get(0).getKey());
    }

    @Test
    void scanRejectsLocalStore() {
        ReflectionTestUtils.setField(service, "qiniuEnabled", false);

        BizException error = assertThrows(BizException.class,
                () -> service.scan(new FileOrphanScanReqVO()));

        assertTrue(error.getMessage().contains("七牛"));
    }

    @Test
    void backfillSkipsRegisteredAndMissingObjects() {
        when(fileInfoService.findAnyByStoragePath("avatar/1.png")).thenReturn(file(1L, 0, 0));
        when(fileInfoService.findAnyByStoragePath("avatar/2.png")).thenReturn(null);
        when(fileInfoService.findAnyByStoragePath("avatar/3.png")).thenReturn(null);
        Map<String, Map<String, String>> metadata = new LinkedHashMap<>();
        metadata.put("avatar/3.png", Map.of("size", "256", "mimeType", "image/png"));
        when(qiniu.getFileInfo(List.of("avatar/2.png", "avatar/3.png"))).thenReturn(metadata);
        FileRespVO saved = new FileRespVO();
        saved.setId(33L);
        saved.setScene("avatar");
        when(fileInfoService.register(any(FileCreateReqVO.class), isNull(), eq("system"))).thenReturn(saved);

        FileOrphanBackfillReqVO req = new FileOrphanBackfillReqVO();
        req.setKeys(List.of("avatar/1.png", "avatar/2.png", "avatar/3.png"));
        req.setDryRun(false);

        FileOrphanBackfillRespVO resp = service.backfill(req);

        assertEquals(3, resp.getTotal().intValue());
        assertEquals(1, resp.getRegistered().intValue());
        assertEquals(2, resp.getSkipped().intValue());
        assertEquals("已登记", resp.getList().get(0).getReason());
        assertEquals("存储中不存在", resp.getList().get(1).getReason());
        assertEquals(33L, resp.getList().get(2).getFileId().longValue());

        ArgumentCaptor<FileCreateReqVO> captor = ArgumentCaptor.forClass(FileCreateReqVO.class);
        verify(fileInfoService).register(captor.capture(), isNull(), eq("system"));
        assertEquals("avatar/3.png", captor.getValue().getStoragePath());
        assertEquals(Integer.valueOf(3), captor.getValue().getThirdPartyName());
        assertEquals("image/png", captor.getValue().getMimeType());
        assertEquals(256L, captor.getValue().getFileSize().longValue());
        assertEquals("avatar", captor.getValue().getScene());
    }

    @Test
    void backfillDryRunDoesNotWrite() {
        when(fileInfoService.findAnyByStoragePath("avatar/9.png")).thenReturn(null);
        when(qiniu.getFileInfo(List.of("avatar/9.png")))
                .thenReturn(Map.of("avatar/9.png", Map.of("size", "64", "mimeType", "image/png")));

        FileOrphanBackfillReqVO req = new FileOrphanBackfillReqVO();
        req.setKeys(List.of("avatar/9.png"));

        FileOrphanBackfillRespVO resp = service.backfill(req);

        assertTrue(resp.getDryRun());
        assertEquals(0, resp.getRegistered().intValue());
        assertEquals("dryRun", resp.getList().get(0).getReason());
        verify(fileInfoService, never()).register(any(), any(), any());
    }

    @Test
    void backfillNormalizesCdnUrlToBareKey() {
        when(fileInfoService.findAnyByStoragePath("moment/5.png")).thenReturn(null);
        when(qiniu.getFileInfo(List.of("moment/5.png")))
                .thenReturn(Map.of("moment/5.png", Map.of("size", "32", "mimeType", "image/png")));

        FileOrphanBackfillReqVO req = new FileOrphanBackfillReqVO();
        req.setKeys(List.of("https://img.soberup.top/moment/5.png"));
        req.setDryRun(true);

        FileOrphanBackfillRespVO resp = service.backfill(req);

        assertEquals("moment/5.png", resp.getList().get(0).getKey());
        assertEquals("moment", resp.getList().get(0).getScene());
    }

    @Test
    void backfillRejectsDirectoryPlaceholderKey() {
        FileOrphanBackfillReqVO req = new FileOrphanBackfillReqVO();
        req.setKeys(List.of("qingsi/emoji/"));

        FileOrphanBackfillRespVO resp = service.backfill(req);

        assertEquals(1, resp.getSkipped().intValue());
        assertEquals("目录占位对象", resp.getList().get(0).getReason());
        verify(qiniu, never()).getFileInfo(anyList());
    }

    @Test
    void backfillRejectsBlankKeys() {
        FileOrphanBackfillReqVO req = new FileOrphanBackfillReqVO();
        req.setKeys(List.of("  "));

        BizException error = assertThrows(BizException.class, () -> service.backfill(req));

        assertTrue(error.getMessage().contains("key"));
        verifyNoInteractions(qiniu);
    }

    @Test
    void unreferencedFiltersZeroRefCountAndAge() {
        when(fileInfoService.page(any(IPage.class), any())).thenReturn(new Page<FileInfo>(1, 20));
        FileUnreferencedReqVO req = new FileUnreferencedReqVO();
        req.setDays(7);

        service.pageUnreferenced(req);

        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(fileInfoService).page(any(IPage.class), captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("ref_count"));
        assertTrue(sql.contains("create_time"));
        assertTrue(sql.contains("scene") || sql.contains("original_name") || sql.contains("status"));
    }

    @Test
    void unreferencedNormalizesMissingDaysToDefault() {
        when(fileInfoService.page(any(IPage.class), any())).thenReturn(new Page<FileInfo>(1, 20));
        FileUnreferencedReqVO req = new FileUnreferencedReqVO();
        req.setDays(null);

        service.pageUnreferenced(req);

        assertEquals(Integer.valueOf(7), req.getDays());
    }

    @Test
    void batchDryRunReportsWithoutWriting() {
        when(fileInfoService.findAny(7L)).thenReturn(file(7L, 0, 0));
        when(fileInfoService.findAny(8L)).thenReturn(file(8L, 2, 0));
        FileBatchReqVO req = new FileBatchReqVO();
        req.setIds(List.of(7L, 8L));

        FileBatchRespVO resp = service.batch(req);

        assertTrue(resp.getDryRun());
        assertEquals(2, resp.getTotal().intValue());
        assertEquals(1, resp.getDeleted().intValue());
        assertEquals(1, resp.getSkipped().intValue());
        assertEquals("soft-delete", resp.getList().get(0).getAction());
        assertTrue(resp.getList().get(1).getReason().contains("引用"));
        verify(fileInfoService, never()).softDelete(any());
        verify(fileInfoService, never()).purge(any());
    }

    @Test
    void batchSoftModeSkipsFilesAlreadyInRecycleBin() {
        when(fileInfoService.findAny(7L)).thenReturn(file(7L, 0, 1));
        FileBatchReqVO req = new FileBatchReqVO();
        req.setIds(List.of(7L));
        req.setDryRun(false);

        FileBatchRespVO resp = service.batch(req);

        assertEquals(0, resp.getDeleted().intValue());
        assertEquals("已在回收站", resp.getList().get(0).getReason());
        verify(fileInfoService, never()).softDelete(any());
    }

    @Test
    void batchForcePurgesUnreferencedAndSkipsReferenced() {
        FileInfo active = file(7L, 0, 0);
        FileInfo recycled = file(8L, 0, 1);
        when(fileInfoService.findAny(7L)).thenReturn(active);
        when(fileInfoService.findAny(8L)).thenReturn(recycled);
        when(fileInfoService.findAny(9L)).thenReturn(file(9L, 1, 0));
        FileBatchReqVO req = new FileBatchReqVO();
        req.setIds(List.of(7L, 8L, 9L));
        req.setDryRun(false);
        req.setForce(true);

        FileBatchRespVO resp = service.batch(req);

        assertEquals(2, resp.getDeleted().intValue());
        assertEquals(1, resp.getSkipped().intValue());
        assertEquals("purge", resp.getList().get(0).getAction());
        verify(fileInfoService).purge(active);
        verify(fileInfoService).purge(recycled);
        verify(fileInfoService, never()).softDelete(any());
    }

    @Test
    void batchSkipsUnknownAndDeduplicatesIds() {
        when(fileInfoService.findAny(7L)).thenReturn(file(7L, 0, 0));
        when(fileInfoService.findAny(8L)).thenReturn(null);
        FileBatchReqVO req = new FileBatchReqVO();
        req.setIds(List.of(7L, 7L, 8L));

        FileBatchRespVO resp = service.batch(req);

        assertEquals(2, resp.getTotal().intValue());
        assertEquals(1, resp.getDeleted().intValue());
        assertEquals(1, resp.getSkipped().intValue());
        assertEquals("文件不存在", resp.getList().get(1).getReason());
    }
}
