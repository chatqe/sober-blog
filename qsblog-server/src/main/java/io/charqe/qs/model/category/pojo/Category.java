package io.charqe.qs.model.category.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 分类表
 * </p>
 *
 * @author joy
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 分类图标
     */
    private String icon;

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
     * 是否删除[0:未删除，1:已删除]
     */
    @TableLogic
    private Boolean deleted;
}
