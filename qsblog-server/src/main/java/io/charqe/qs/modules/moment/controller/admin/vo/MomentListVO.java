package io.charqe.qs.modules.moment.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动态列表响应VO（Admin）
 *
 * @author Joy
 * @since 2026-06-25
 */
@Data
@Schema(description = "动态列表VO")
public class MomentListVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "发布者ID")
    private Long userId;

    @Schema(description = "内容摘要（截取前50字）")
    private String contentSummary;

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

    @Schema(description = "创建时间")
    private String createTime;
}
