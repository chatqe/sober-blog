package io.charqe.qs.modules.behavior.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户行为状态表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("behavior_state")
public class BehaviorState {

    /** 行为状态：正常 */
    public static final int BEH_ACTIVE = 1;
    /** 行为状态：已取消 */
    public static final int BEH_CANCELLED = 2;

    /**
     * 主键（自增）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 行为类型
     */
    @TableField("action")
    private Integer action;

    /**
     * 业务ID
     */
    @TableField("biz_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long bizId;

    /**
     * 业务主体类型
     */
    @TableField("biz_type")
    private Integer bizType;
    /**
     * 行为状态 1-正常 2-取消
     */
    @TableField("status")
    private Integer status;
    /**
     * 互斥标识  lod为 赞/踩
     */
    @TableField("mutex_key")
    private String mutexKey;


    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
