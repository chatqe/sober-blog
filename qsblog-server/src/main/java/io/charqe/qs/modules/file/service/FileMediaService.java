package io.charqe.qs.modules.file.service;

import io.charqe.qs.modules.file.controller.admin.vo.FileDetailRespVO;

/**
 * 媒体库读取：访问地址、ETag 与派生图均由存储侧能力推导，不落派生文件。
 *
 * @author Joy
 * @since 2026-10-26
 */
public interface FileMediaService {

    /**
     * 媒体详情。
     *
     * @param id 文件ID
     * @return 未登记或已进回收站时返回 {@code null}，由入口按 404 语义处理
     */
    FileDetailRespVO detail(Long id);
}
