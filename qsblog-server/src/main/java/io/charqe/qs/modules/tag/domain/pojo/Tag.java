package io.charqe.qs.modules.tag.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tag")
public class Tag extends BaseDO {

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 标签名称
     */
    @TableField("name")
    private String name;

    /**
     * 标签描述
     */
    @TableField("desc")
    private String desc;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新人
     */
    @TableField("update_by")
    private String updateBy;

}