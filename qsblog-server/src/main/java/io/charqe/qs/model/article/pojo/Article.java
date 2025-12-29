package io.charqe.qs.model.article.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文章表
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("article")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 分类ID
     */
    @TableField("sort_id")
    private Integer sortId;

    /**
     * 标签ID
     */
    @TableField("label_id")
    private Integer labelId;

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
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 最终修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 最终修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    /**
     * 是否启用[0:未删除，1:已删除]
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted;


    public Integer getWeight() {
        return viewCount + likeCount * 2;
    }
}
