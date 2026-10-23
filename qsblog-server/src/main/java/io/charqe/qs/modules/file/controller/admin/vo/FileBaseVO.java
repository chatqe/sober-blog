package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;

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
    private String originalName;

    /**
     * 文件路径
     */
    @NotBlank(message = "存储路径不能为空")
    private String storagePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    @NotBlank(message = "MIME 类型不能为空")
    private String mimeType;

    /**
     * 存储类型
     */
    private Integer thirdPartyName;

    /**
     * CDN 访问前缀，可包含路径；为空时使用存储路径
     */
    private String cdnDomain;
}
