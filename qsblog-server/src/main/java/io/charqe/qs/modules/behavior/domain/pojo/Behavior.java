package io.charqe.qs.modules.behavior.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户行为表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_behavior")
public class Behavior extends BaseDO {

    /**
     * 用户ID
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 行为类型
     */
    @TableField("behavior_type")
    private Integer behaviorType;

    /**
     * 业务ID
     */
    @TableField("biz_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long bizId;

    /**
     * 业务类型
     */
    @TableField("biz_type")
    private Integer bizType;

    /**
     * 用户IP
     */
    @TableField("ip")
    private String ip;

    /**
     * 用户代理
     */
    @TableField("ua")
    private String ua;

}
