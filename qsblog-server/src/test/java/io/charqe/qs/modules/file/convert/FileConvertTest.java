package io.charqe.qs.modules.file.convert;

import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 直接验证 MapStruct 编译产物，不加载数据库、Redis 或 Spring 容器。 */
class FileConvertTest {
    private final FileConvert converter = FileConvert.INSTANCE;

    private FileCreateReqVO request() {
        FileCreateReqVO request = new FileCreateReqVO();
        request.setOriginalName("文章封面.PNG");
        request.setStoragePath("cover/user22-123.PNG");
        request.setMimeType("image/png");
        request.setFileSize(1024L);
        request.setThirdPartyName(3);
        request.setCdnDomain("https://cdn.example.invalid/assets/");
        return request;
    }

    @Test
    void mapsUploadMetadataWithoutLosingFields() {
        FileInfo file = converter.convert(request());
        assertEquals("文章封面.PNG", file.getOriginalName());
        assertEquals("cover/user22-123.PNG", file.getStoragePath());
        assertNull(file.getStoredName());
        assertEquals("image/png", file.getMimeType());
        assertNull(file.getFileCategory());
        assertNull(file.getFileExt());
        assertEquals(1024L, file.getFileSize());
        assertEquals(3, file.getThirdPartyName());
        assertEquals("https://cdn.example.invalid/assets/", file.getCdnDomain());
        // 输入不提供的服务端元数据不能伪造。
        assertNull(file.getFileHash());
        assertNull(file.getOwnerId());
        assertNull(file.getBucket());
    }

    @Test
    void responseRestoresTheSameContract() {
        FileCreateReqVO request = request();
        FileInfo file = converter.convert(request);
        file.setId(8L);
        FileRespVO result = converter.convert(file);
        assertEquals(8L, result.getId());
        assertEquals(request.getOriginalName(), result.getOriginalName());
        assertEquals(request.getStoragePath(), result.getStoragePath());
        assertEquals(request.getFileSize(), result.getFileSize());
        assertEquals(request.getMimeType(), result.getMimeType());
        assertEquals(request.getThirdPartyName(), result.getThirdPartyName());
        assertEquals(request.getCdnDomain(), result.getCdnDomain());
    }

    @Test
    void supportsLocalPathsAndLeadingSlash() {
        FileCreateReqVO request = request();
        request.setStoragePath("/uploads/file.png");
        request.setCdnDomain("/");
        request.setThirdPartyName(1);
        FileInfo file = converter.convert(request);
        assertEquals("/", file.getCdnDomain());
        assertEquals("/uploads/file.png", converter.convert(file).getStoragePath());
        assertEquals("/", converter.convert(file).getCdnDomain());
    }

    @Test
    void absentCdnKeepsPathAndNullInputRemainsNull() {
        FileCreateReqVO request = request();
        request.setCdnDomain(null);
        assertNull(converter.convert(request).getCdnDomain());
        assertEquals(request.getStoragePath(), converter.convert(converter.convert(request)).getStoragePath());
        assertNull(converter.convert((FileCreateReqVO) null));
        assertNull(converter.convert((FileInfo) null));
    }

    @Test
    void responseCopiesDerivedFieldsWithoutAliasesOrInternalMetadata() {
        FileInfo file = converter.convert(request());
        file.setStoredName("server-name.png");
        file.setFileCategory("image");
        file.setFileExt("png");
        file.setFileHash("internal-hash");
        file.setOwnerId(99L);
        file.setBucket("internal-bucket");
        var json = new ObjectMapper().valueToTree(converter.convert(file));
        assertEquals("server-name.png", json.path("storedName").asText());
        assertEquals("image", json.path("fileCategory").asText());
        assertEquals("png", json.path("fileExt").asText());
        Set<String> fields = new HashSet<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id", "originalName", "storagePath", "fileSize", "mimeType",
                "thirdPartyName", "cdnDomain", "storedName", "fileCategory", "fileExt",
                "fileHash", "scene"), fields);
    }
}
