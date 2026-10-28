package io.charqe.qs.modules.file.service;

import io.charqe.qs.infra.storage.FileStorageService;
import io.charqe.qs.infra.storage.StoreService;
import io.charqe.qs.modules.file.domain.dao.FileStorageMapper;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 永久删除的存储清理：元数据必须落删，存储清理失败只告警不阻断。
 */
class FileInfoStoragePurgeTest {

    private FileInfoServiceImpl service;
    private FileStorageMapper mapper;
    private FileStorageService storeService;
    private StoreService qiniu;

    @BeforeEach
    void setup() {
        service = spy(new FileInfoServiceImpl());
        mapper = mock(FileStorageMapper.class);
        storeService = mock(FileStorageService.class);
        qiniu = mock(StoreService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "storeService", storeService);
        when(storeService.getFileStorage("qiniu")).thenReturn(qiniu);
    }

    private FileInfo file(Integer thirdPartyName, String storagePath) {
        var file = new FileInfo();
        file.setId(9L);
        file.setThirdPartyName(thirdPartyName);
        file.setStoragePath(storagePath);
        return file;
    }

    @Test
    void purgeDeletesStoredObjectThenMetadata() {
        var file = file(3, "avatar/31/202610/01ABC.png");

        service.purge(file);

        verify(qiniu).deleteFile(List.of("avatar/31/202610/01ABC.png"));
        verify(mapper).purgeById(9L);
    }

    @Test
    void storageFailureDoesNotBlockMetadataDeletion() {
        var file = file(3, "avatar/31/202610/01ABC.png");
        doThrow(new RuntimeException("qiniu down")).when(qiniu).deleteFile(anyList());

        service.purge(file);

        verify(mapper).purgeById(9L);
    }

    @Test
    void unknownStorageTypeSkipsObjectDeletion() {
        var file = file(2, "cos/only.png");

        service.purge(file);

        verify(storeService, never()).getFileStorage(any());
        verify(mapper).purgeById(9L);
    }

    @Test
    void blankStoragePathSkipsObjectDeletion() {
        var file = file(3, "");

        service.purge(file);

        verify(qiniu, never()).deleteFile(anyList());
        verify(mapper).purgeById(9L);
    }
}
