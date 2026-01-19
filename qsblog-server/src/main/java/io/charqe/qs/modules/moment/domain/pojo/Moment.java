package io.charqe.qs.modules.moment.domain.pojo;

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
@TableName("moment")
public class Moment extends BaseDO {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 内容
     */
    @TableField("content")
    private String content;

    /**
     * 是否公开[0:仅自己可见，1:所有人可见]
     */
    @TableField("is_public")
    private Boolean isPublic;

    /**
     * 类型
     */
    @TableField("type")
    private String type;

    /**
     * 来源标识
     */
    @TableField("source")
    private Long source;

}
