package io.charqe.qs.modules.moment.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 动态详情响应VO（Admin）
 *
 * @author Joy
 * @since 2026-06-25
 */
@Data
@Schema(description = "动态详情VO")
public class MomentDetailVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "发布者ID")
    private Long userId;

    @Schema(description = "发布者昵称")
    private String authorName;

    @Schema(description = "发布者头像URL")
    private String authorAvatar;

    @Schema(description = "纯文本内容")
    private String content;

    @Schema(description = "是否有文本")
    private Boolean hasText;

    @Schema(description = "是否有图片")
    private Boolean hasImages;

    @Schema(description = "是否有视频")
    private Boolean hasVideos;

    @Schema(description = "是否有音频")
    private Boolean hasAudios;

    @Schema(description = "点赞数")
    private Integer likesCount;

    @Schema(description = "评论数")
    private Integer commentsCount;

    @Schema(description = "浏览数")
    private Integer viewsCount;

    @Schema(description = "状态: 0-草稿 1-正常 2-审核中 3-违规下架")
    private Integer status;

    @Schema(description = "可见性: 0-私密 1-好友 2-公开")
    private Integer visibility;

    @Schema(description = "来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音")
    private Integer source;

    @Schema(description = "三方平台原始动态ID")
    private String sourceExternalId;

    @Schema(description = "三方平台用户ID")
    private String sourceUserId;

    @Schema(description = "删除时间")
    private String delTime;

    @Schema(description = "删除操作人ID")
    private Long delBy;

    @Schema(description = "审核/驳回原因")
    private String auditReason;

    @Schema(description = "审核时间")
    private String auditedAt;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;

    @Schema(description = "媒体文件列表")
    private List<MomentMediaVO> mediaList;

    /**
     * 媒体文件VO
     */
    @Data
    @Schema(description = "媒体文件VO")
    public static class MomentMediaVO {

        @Schema(description = "ID")
        private Long id;

        @Schema(description = "文件ID")
        private Long fileId;

        @Schema(description = "媒体类型: 1-图片 2-视频 3-音频")
        private Integer mediaType;

        @Schema(description = "排序权重")
        private Integer sortOrder;
    }
}
