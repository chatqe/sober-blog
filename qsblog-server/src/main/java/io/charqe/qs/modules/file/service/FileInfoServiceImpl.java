package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.convert.FileConvert;
import io.charqe.qs.modules.file.domain.dao.FileStorageMapper;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import io.charqe.qs.modules.file.enums.FileSceneEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 文件存储表 服务实现类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Service
public class FileInfoServiceImpl extends CoreServiceImpl<FileStorageMapper, FileInfo> implements FileInfoService {


    @Value("${store.type:local}")
    private String storeType;

    @Value("${qiniu.bucket:}")
    private String qiniuBucket;

    @Override
    public FileRespVO save(FileCreateReqVO vo) {
        return register(vo, null, null);
    }

    @Override
    public FileRespVO register(FileCreateReqVO vo, Long ownerId, String ownerType) {
        FileInfo file = FileConvert.INSTANCE.convert(vo);
        file.setStoredName(StringUtils.getFilename(file.getStoragePath()));
        String extension = StringUtils.getFilenameExtension(file.getStoragePath());
        file.setFileExt(extension == null ? "" : extension.toLowerCase(Locale.ROOT));
        file.setFileCategory(category(file.getMimeType()));
        if (!StringUtils.hasText(file.getOriginalName())) {
            file.setOriginalName(file.getStoredName());
        }
        if (file.getThirdPartyName() == null) {
            file.setThirdPartyName(defaultStorageType());
        }
        if (!StringUtils.hasText(file.getBucket())) {
            file.setBucket(Integer.valueOf(3).equals(file.getThirdPartyName()) ? nullToEmpty(qiniuBucket) : "");
        }
        file.setScene(FileSceneEnum.normalize(file.getScene()));
        if (StringUtils.hasText(file.getFileHash())) {
            file.setFileHash(file.getFileHash().toLowerCase(Locale.ROOT));
        }
        file.setOwnerId(ownerId);
        file.setOwnerType(StringUtils.hasText(ownerType) ? ownerType : (ownerId == null ? "system" : "user"));
        if (file.getStatus() == null) {
            file.setStatus(1);
        }
        file.setRefCount(0);
        if (file.getTenantId() == null) {
            file.setTenantId(0);
        }

        save(file);
        return FileConvert.INSTANCE.convert(file);
    }

    /** 未指定存储类型时按当前 store.type 回填，避免与多存储统计错位。 */
    private Integer defaultStorageType() {
        return "qiniu".equalsIgnoreCase(storeType) ? 3 : 1;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** 元数据派生属于业务层，转换层仅复制同名字段。 */
    private String category(String mimeType) {
        if (mimeType == null) return "other";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        if (mimeType.startsWith("text/") || mimeType.equals("application/pdf")) return "document";
        return "other";
    }
}