package io.charqe.qs.model.articlelike.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文章点赞表
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("article_like")
public class ArticleLike implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 文章ID
     */
    private Integer articleId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 最终修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 最终修改人
     */
    private String updateBy;

    /**
     * 是否启用[0:未删除，1:已删除]
     */
    @TableLogic
    private Boolean deleted;

}
