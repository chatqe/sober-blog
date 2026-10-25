package io.charqe.qs.modules.file.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 上传凭证响应：前端拿 {@code key} 原样上传，再凭同一 key 登记。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@Builder
public class FileUploadTokenRespVO {

    @Schema(description = "存储类型：local/qiniu")
    private String storeType;

    @Schema(description = "对象存储上传凭证；本地存储时为空")
    private String token;

    @Schema(description = "服务端生成的存储路径，上传与登记必须使用同一值")
    private String key;

    @Schema(description = "上传地址；前端可按自身配置覆盖")
    private String uploadUrl;

    @Schema(description = "访问前缀（CDN 域名，含末尾斜杠）")
    private String downloadUrl;

    @Schema(description = "单次上传大小上限（字节）")
    private Long fsizeLimit;

    @Schema(description = "允许的 MIME 类型")
    private String mimeLimit;

    @Schema(description = "1=禁止覆盖同 Key 对象")
    private Integer insertOnly;

    @Schema(description = "登记时需携带的哈希算法，目前固定 sha256")
    private String hashAlgo;

    @Schema(description = "凭证过期时间（epoch 秒）")
    private Long expiresAt;
}
