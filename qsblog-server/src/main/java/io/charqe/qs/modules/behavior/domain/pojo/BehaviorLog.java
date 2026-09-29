package io.charqe.qs.modules.behavior.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户行为流水表
 *
 * @author Joy
 * @since 2026-03-15 19:53
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("behavior_log")
public class BehaviorLog extends BaseDO {
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
     * 匿名标识（未登录用户：设备ID/Token）
     */
    @TableField("guest_id")
    private String guestId;
}
