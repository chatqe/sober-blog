package io.charqe.qs.modules.article.controller.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台Article列表Vo
 *
 * @author Joy
 * @since 2026-09-21 22:02
 */
@Data
@Schema(description = "B端文章分页列表项")
public class AdminArticleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(description = "文章ID")
    private Long id;

    @Schema(description = "博文标题")
    private String articleTitle;

    @Schema(description = "封面")
    private String articleCover;

    @Schema(description = "分类ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long tagId;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "作者用户ID")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    @Schema(description = "作者名称")
    private String authorName;

    // @Schema(description = "作者头像")
    // private String authorAvatar;

    @Schema(description = "是否可见[0:否，1:是]")
    private Boolean viewStatus;

    @Schema(description = "是否推荐[0:否，1:是]")
    private Boolean recommendStatus;

    @Schema(description = "是否启用评论[0:否，1:是]")
    private Boolean commentStatus;

    @Schema(description = "发布状态[0:草稿，1:已发布]")
    private Integer publishStatus;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "文章锁定提示")
    private String tips;

    @Schema(description = "视频链接，列表不需要可删除")
    private String videoUrl;

    @Schema(description = "是否有视频")
    private Boolean hasVideo;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "最终修改时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "最终修改人")
    private String updateBy;
}
