package io.charqe.qs.modules.file.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 媒体库详情：在登记信息之外补访问地址、ETag 与派生图。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
@Schema(description = "媒体库详情")
public class FileDetailRespVO extends FileRespVO {

    @Schema(description = "拥有者ID，补登记对象为空")
    private Long ownerId;

    @Schema(description = "拥有者类型 user/admin/system")
    private String ownerType;

    @Schema(description = "存储桶")
    private String bucket;

    @Schema(description = "被引用次数")
    private Integer refCount;

    @Schema(description = "1-正常 0-禁用 2-审核中")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "进入回收站时间，存活对象为空")
    private LocalDateTime deletedAt;

    @Schema(description = "完整访问地址，CDN 前缀 + 存储路径")
    private String visitPath;

    @Schema(description = "内容摘要（SHA256），无 file_hash 时为空")
    private String etag;

    @Schema(description = "派生策略：transform-按需变换 / none-仅原图")
    private String variantMode;

    @Schema(description = "派生图地址，variantMode=none 时为空")
    private FileVariantsVO variants;
}
