package io.charqe.qs.modules.behavior.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@TableName("behavior_cnt")
public class BehaviorCnt extends BaseDO {

    /**
     * 行为主体id
     */
    @TableField("biz_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long bizId;

    /**
     * 主体类型
     */
    @TableField("biz_type")
    private Integer bizType;

    /**
     * 行为类型
     */
    @TableField("action")
    private Integer action;

    /**
     * 行为主体id
     */
    @TableField("user_id")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long userId;

    /**
     * 行为数量
     */
    @TableField
    private Integer count;
    /**
     * 乐观锁 版本号
     */
    @TableField("version")
    private String version;

}
