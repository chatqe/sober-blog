package io.charqe.qs.modules.article.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("article")
public class Article extends BaseDO {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long  userId;

    /**
     * 分类ID
     */
    @TableField("sort_id")
    private Long sortId;

    /**
     * 标签ID
     */
    @TableField("label_id")
    private Long labelId;

    /**
     * 封面
     */
    @TableField("article_cover")
    private String articleCover;

    /**
     * 博文标题
     */
    @TableField("article_title")
    private String articleTitle;

    /**
     * 博文内容
     */
    @TableField("article_content")
    private String articleContent;

    /**
     * 视频链接
     */
    @TableField("video_url")
    private String videoUrl;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 提示
     */
    @TableField("tips")
    private String tips;

    /**
     * 是否可见[0:否，1:是]
     */
    @TableField("view_status")
    private Boolean viewStatus;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 是否启用评论[0:否，1:是]
     */
    @TableField("comment_status")
    private Boolean commentStatus;

    /**
     * 是否推荐[0:否，1:是]
     */
    @TableField("recommend_status")
    private Boolean recommendStatus;

    /**
     * 最终修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;


    public Integer getWeight() {
        return viewCount + likeCount * 2;
    }
}
