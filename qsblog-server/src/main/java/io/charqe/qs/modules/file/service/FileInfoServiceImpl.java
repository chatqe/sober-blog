package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.convert.FileConvert;
import io.charqe.qs.modules.file.domain.dao.FileStorageMapper;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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

        FileInfo convert = FileConvert.INSTANCE.convert(vo);

        save(convert);
        return FileConvert.INSTANCE.convert(convert);
    }
}