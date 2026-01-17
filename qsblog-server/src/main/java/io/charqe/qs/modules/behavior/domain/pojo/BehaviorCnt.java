package io.charqe.qs.modules.behavior.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户行为统计表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_behavior_cnt")
public class BehaviorCnt extends BaseDO {

    /**
     * 统计日期
     */
    @TableField("stat_date")
    private String statDate;

    /**
     * 行为类型
     */
    @TableField("behavior_type")
    private Integer behaviorType;

    /**
     * 业务类型
     */
    @TableField("biz_type")
    private Integer bizType;

    /**
     * 访问次数
     */
    @TableField("count")
    private Integer count;

}
