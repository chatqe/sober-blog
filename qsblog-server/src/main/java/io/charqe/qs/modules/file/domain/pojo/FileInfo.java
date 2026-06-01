package io.charqe.qs.modules.file.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件存储表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("file_info")
public class FileInfo extends BaseDO {

    /**
     * 租户ID（SaaS多租户扩展）
     */
    private Integer tenantId;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    // ========== 文件核心元数据 ==========

    /** 原始文件名（用户上传的文件名） */
    private String originalName;

    /** 存储文件名（服务端生成 UUID.ext） */
    private String storedName;

    /** 文件SHA256哈希（去重+秒传） */
    private String fileHash;

    /** 存储路径（如 2024/06/18/abc123.jpg） */
    private String storagePath;

    /** CDN域名（为空时使用默认域名） */
    private String cdnDomain;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 文件分类: image/video/document/audio/other */
    private String fileCategory;

    /** MIME类型（如 image/jpeg） */
    private String mimeType;

    /** 文件扩展名(小写, 如 jpg) */
    private String fileExt;

    /** 图片/视频宽度(px) */
    private Integer width;

    /** 图片/视频高度(px) */
    private Integer height;

    /** 音视频时长(秒) */
    private Integer duration;

    // ========== 存储适配 ==========

    /** 存储类型 1-本地 2-腾讯COS 3-七牛云 4-S3 5-阿里OSS 6-Minio */
    private Integer thirdPartyName;

    /** Bucket名称 */
    private String bucket;

    /** 存储区域 */
    private String region;

    /** 第三方平台文件ID */
    private String thirdPartyId;

    // ========== 业务归属 ==========

    /** 文件拥有者ID */
    private Long ownerId;

    /** 拥有者类型 user/admin/system */
    private String ownerType;

    // ========== 状态与引用 ==========

    /** 1-正常 0-禁用 2-审核中 */
    private Integer status;

    /** 被业务引用次数 */
    private Integer refCount;

}