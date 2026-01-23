package io.charqe.qs.common.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用实体基类 Base Domain Object 业务领域对象基类
 *
 * @author Joy
 * @since 2026-01-17 16:56
 */
@Data
public abstract class BaseDO implements Serializable {
    /**
     * 主键（自增）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 逻辑删除标识（0-未删，1-已删）
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer isDel;

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

    /**
     * 乐观锁版本号（并发控制，新增）
     */
//     @Version
//     @TableField(fill = FieldFill.INSERT)
//     private Integer version;
//
}
