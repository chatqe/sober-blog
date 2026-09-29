package io.charqe.qs.modules.moment.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 动态详情VO（Web端）
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

    @Schema(description = "可见性: 0-私密 1-好友 2-公开")
    private Integer visibility;

    @Schema(description = "来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音")
    private Integer source;

    @Schema(description = "当前用户是否已点赞")
    private Boolean liked;

    @Schema(description = "媒体文件列表")
    private List<MomentMediaVO> mediaList;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;
}
