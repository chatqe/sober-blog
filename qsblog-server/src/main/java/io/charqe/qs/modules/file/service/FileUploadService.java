package io.charqe.qs.modules.file.service;

import io.charqe.qs.modules.file.controller.web.vo.FileConfigVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenReqVO;
import io.charqe.qs.modules.file.controller.web.vo.FileUploadTokenRespVO;
import io.charqe.qs.modules.user.domain.pojo.User;

/**
 * 文件上传策略服务：负责下发服务端 Key、上传凭证与模块级配置。
 *
 * @author Joy
 * @since 2026-10-26
 */
public interface FileUploadService {

    /**
     * 生成服务端 Key 与上传凭证。
     *
     * @param req  文件元数据
     * @param user 当前会话用户，用于决定拥有者与可上传场景
     */
    FileUploadTokenRespVO token(FileUploadTokenReqVO req, User user);

    /** 文件模块公开配置。 */
    FileConfigVO config();
}
