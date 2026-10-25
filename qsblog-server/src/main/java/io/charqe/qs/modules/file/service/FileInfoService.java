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

    /**
     * 登记文件元数据：拥有者由调用方按会话决定，服务端补齐存储桶/场景/分类等字段。
     *
     * @param vo        上传后回填的元数据
     * @param ownerId   拥有者 ID，系统任务可为空
     * @param ownerType user/admin/system，为空时按 ownerId 推断
     */
    FileRespVO register(FileCreateReqVO vo, Long ownerId, String ownerType);
}