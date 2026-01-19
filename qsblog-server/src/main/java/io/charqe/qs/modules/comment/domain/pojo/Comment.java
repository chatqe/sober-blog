package io.charqe.qs.modules.comment.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("comment")
public class Comment extends BaseDO {

    /**
     * 评论来源标识
     */
    @TableField("source")
    private Long source;

    /**
     * 父评论ID
     */
    @TableField("parent_comment_id")
    private Long parentCommentId;

    /**
     * 评论来源类型
     */
    @TableField("type")
    private String type;

    /**
     * 发表用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 父发表用户名ID
     */
    @TableField("parent_user_id")
    private Long parentUserId;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 评论内容
     */
    @TableField("comment_content")
    private String commentContent;

    /**
     * 评论额外信息
     */
    @TableField("comment_info")
    private String commentInfo;

    /**
     * 楼层评论ID
     */
    @TableField("floor_comment_id")
    private Long floorCommentId;

}

