package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

/**
 * file基础VO
 *
 * @author Joy
 * @since 2026-01-21 18:45
 */
@Data
public class FileBaseVO {
    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 存储类型
     */
    private Integer storageType;

    /**
     * 访问URL
     */
    private String accessUrl;
}
