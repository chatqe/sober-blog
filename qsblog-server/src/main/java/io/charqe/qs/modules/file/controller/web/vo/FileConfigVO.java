package io.charqe.qs.modules.file.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件模块公开配置：前端据此确定存储类型、限额与可用场景，避免硬编码。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@Builder
public class FileConfigVO {

    @Schema(description = "存储类型：local/qiniu")
    private String storeType;

    @Schema(description = "七牛云是否已启用")
    private Boolean qiniuEnabled;

    @Schema(description = "上传地址")
    private String uploadUrl;

    @Schema(description = "访问前缀（CDN 域名，含末尾斜杠）")
    private String downloadUrl;

    @Schema(description = "单次上传大小上限（字节）")
    private Long fsizeLimit;

    @Schema(description = "当前会话可直接上传的场景")
    private List<String> clientScenes;

    @Schema(description = "登记时需携带的哈希算法")
    private String hashAlgo;

    @Schema(description = "上传凭证有效期（秒）")
    private Long tokenTtl;
}
