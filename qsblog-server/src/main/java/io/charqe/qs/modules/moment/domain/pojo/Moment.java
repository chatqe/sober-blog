package io.charqe.qs.modules.moment.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 动态主实体
 *
 * @author Joy
 * @since 2026-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("moments")
public class Moment extends BaseDO {

    /**
     * 发布者ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 纯文本内容（支持emoji）
     */
    @TableField("content")
    private String content;

    /**
     * 是否有文本[0:无 1:有]
     */
    @TableField("has_text")
    private Boolean hasText;

    /**
     * 是否有图片[0:无 1:有]
     */
    @TableField("has_images")
    private Boolean hasImages;

    /**
     * 是否有视频[0:无 1:有]
     */
    @TableField("has_videos")
    private Boolean hasVideos;

    /**
     * 是否有音频[0:无 1:有]
     */
    @TableField("has_audios")
    private Boolean hasAudios;

    /**
     * 点赞数（冗余，由行为模块异步更新）
     */
    @TableField("likes_count")
    private Integer likesCount;

    /**
     * 评论数（冗余，由行为模块异步更新）
     */
    @TableField("comments_count")
    private Integer commentsCount;

    /**
     * 浏览数（冗余，由行为模块异步更新）
     */
    @TableField("views_count")
    private Integer viewsCount;

    /**
     * 动态状态: 0-草稿 1-正常 2-审核中(预留) 3-违规下架(预留)
     */
    @TableField("status")
    private Integer status;

    /**
     * 可见性: 0-私密 1-好友 2-公开
     */
    @TableField("visibility")
    private Integer visibility;

    /**
     * 来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音
     */
    @TableField("source")
    private Integer source;

    /**
     * 三方平台原始动态ID（配合source用于去重）
     */
    @TableField("source_external_id")
    private String sourceExternalId;

    /**
     * 三方平台用户ID（用于账号绑定关系）
     */
    @TableField("source_user_id")
    private String sourceUserId;

    /**
     * 删除时间（NULL表示未删除）
     */
    @TableField("del_time")
    private LocalDateTime delTime;

    /**
     * 删除操作人ID（管理员删除/用户删除追溯）
     */
    @TableField("del_by")
    private Long delBy;

    /**
     * 审核/驳回原因（status=2/3时填写）
     */
    @TableField("audit_reason")
    private String auditReason;

    /**
     * 审核时间
     */
    @TableField("audited_at")
    private LocalDateTime auditedAt;

    /**
     * 非数据库字段：文件列表（查询时关联 moment_media + file_info 填充）
     */
    @TableField(exist = false)
    private java.util.List<MomentMedia> mediaList;
}
