package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.convert.FileConvert;
import io.charqe.qs.modules.file.domain.dao.FileStorageMapper;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
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


    @Override
    public FileRespVO save(FileCreateReqVO vo) {

        FileInfo file = FileConvert.INSTANCE.convert(vo);
        file.setStoredName(StringUtils.getFilename(file.getStoragePath()));
        String extension = StringUtils.getFilenameExtension(file.getStoragePath());
        file.setFileExt(extension == null ? "" : extension.toLowerCase(Locale.ROOT));
        file.setFileCategory(category(file.getMimeType()));

        save(file);
        return FileConvert.INSTANCE.convert(file);
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