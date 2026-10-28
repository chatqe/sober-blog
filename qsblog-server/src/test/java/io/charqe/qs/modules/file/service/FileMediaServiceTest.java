package io.charqe.qs.modules.file.service;

import io.charqe.qs.modules.file.controller.admin.vo.FileDetailRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 媒体详情的地址推导：七牛变换三档、非图片降级、回收站不可见。
 */
class FileMediaServiceTest {

    private FileInfoService fileInfoService;
    private FileMediaServiceImpl service;

    @BeforeEach
    void setup() {
        fileInfoService = mock(FileInfoService.class);
        service = new FileMediaServiceImpl();
        ReflectionTestUtils.setField(service, "fileInfoService", fileInfoService);
        ReflectionTestUtils.setField(service, "qiniuEnabled", true);
        ReflectionTestUtils.setField(service, "qiniuDownloadUrl", "https://img.soberup.top/");
        ReflectionTestUtils.setField(service, "localDownloadUrl", "http://localhost:18081/local/");
    }

    private FileInfo qiniuImage() {
        FileInfo file = new FileInfo();
        file.setId(9L);
        file.setOriginalName("cover.png");
        file.setStoragePath("avatar/3/202610/a.png");
        file.setMimeType("image/png");
        file.setFileSize(1024L);
        file.setThirdPartyName(3);
        file.setScene("avatar");
        file.setOwnerId(3L);
        file.setOwnerType("user");
        file.setStatus(1);
        file.setRefCount(0);
        file.setIsDel(0);
        file.setFileHash("ABCDEF0123");
        return file;
    }

    @Test
    void qiniuImageExposesThreeVariants() {
        when(fileInfoService.findAny(9L)).thenReturn(qiniuImage());

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("https://img.soberup.top/avatar/3/202610/a.png", detail.getVisitPath());
        assertEquals("transform", detail.getVariantMode());
        assertEquals("https://img.soberup.top/avatar/3/202610/a.png?imageView2/2/w/320/q/85",
                detail.getVariants().getThumb());
        assertEquals("https://img.soberup.top/avatar/3/202610/a.png?imageView2/2/w/800/q/85",
                detail.getVariants().getCard());
        assertEquals("https://img.soberup.top/avatar/3/202610/a.png?imageView2/2/w/1600/q/85",
                detail.getVariants().getHero());
        assertEquals("abcdef0123", detail.getEtag());
        assertEquals(Integer.valueOf(0), detail.getRefCount());
    }

    @Test
    void gifKeepsOriginalBecauseTransformWouldDropFrames() {
        FileInfo file = qiniuImage();
        file.setMimeType("image/gif");
        when(fileInfoService.findAny(9L)).thenReturn(file);

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("none", detail.getVariantMode());
        assertNull(detail.getVariants());
        assertEquals("https://img.soberup.top/avatar/3/202610/a.png", detail.getVisitPath());
    }

    @Test
    void nonImageKeepsOriginal() {
        FileInfo file = qiniuImage();
        file.setMimeType("application/pdf");
        when(fileInfoService.findAny(9L)).thenReturn(file);

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("none", detail.getVariantMode());
        assertNull(detail.getVariants());
    }

    @Test
    void qiniuDisabledFallsBackToOriginal() {
        ReflectionTestUtils.setField(service, "qiniuEnabled", false);
        when(fileInfoService.findAny(9L)).thenReturn(qiniuImage());

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("none", detail.getVariantMode());
        assertNull(detail.getVariants());
    }

    @Test
    void localStoreUsesLocalDownloadUrlAndSkipsTransform() {
        FileInfo file = qiniuImage();
        file.setThirdPartyName(1);
        when(fileInfoService.findAny(9L)).thenReturn(file);

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("http://localhost:18081/local/avatar/3/202610/a.png", detail.getVisitPath());
        assertEquals("none", detail.getVariantMode());
    }

    @Test
    void customCdnDomainOverridesDefaultPrefix() {
        FileInfo file = qiniuImage();
        file.setCdnDomain("https://cdn.example.com");
        when(fileInfoService.findAny(9L)).thenReturn(file);

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("https://cdn.example.com/avatar/3/202610/a.png", detail.getVisitPath());
        assertEquals("https://cdn.example.com/avatar/3/202610/a.png?imageView2/2/w/320/q/85",
                detail.getVariants().getThumb());
    }

    @Test
    void keyWithQueryUsesAmpersandForTransform() {
        FileInfo file = qiniuImage();
        file.setStoragePath("avatar/3/202610/a.png?v=2");
        when(fileInfoService.findAny(9L)).thenReturn(file);

        FileDetailRespVO detail = service.detail(9L);

        assertEquals("https://img.soberup.top/avatar/3/202610/a.png?v=2&imageView2/2/w/320/q/85",
                detail.getVariants().getThumb());
    }

    @Test
    void missingHashLeavesEtagNull() {
        FileInfo file = qiniuImage();
        file.setFileHash(null);
        when(fileInfoService.findAny(9L)).thenReturn(file);

        FileDetailRespVO detail = service.detail(9L);

        assertNull(detail.getEtag());
        assertEquals("transform", detail.getVariantMode());
    }

    @Test
    void recycledFileIsNotVisible() {
        FileInfo file = qiniuImage();
        file.setIsDel(1);
        when(fileInfoService.findAny(9L)).thenReturn(file);

        assertNull(service.detail(9L));
    }

    @Test
    void unknownFileReturnsNull() {
        when(fileInfoService.findAny(404L)).thenReturn(null);

        assertNull(service.detail(404L));
    }
}
