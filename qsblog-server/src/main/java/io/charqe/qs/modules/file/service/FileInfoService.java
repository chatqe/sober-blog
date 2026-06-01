package io.charqe.qs.modules.file.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.file.controller.admin.vo.FileCreateReqVO;
import io.charqe.qs.modules.file.controller.admin.vo.FileRespVO;
import io.charqe.qs.modules.file.domain.pojo.FileInfo;

/**
 * 文件存储表 服务类
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
public interface FileInfoService extends CoreService<FileInfo> {


    public FileRespVO save(FileCreateReqVO vo);
}