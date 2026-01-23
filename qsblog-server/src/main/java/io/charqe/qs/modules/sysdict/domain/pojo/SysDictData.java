package io.charqe.qs.modules.sysdict.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_data")
public class SysDictData  {

    /**
     * 字典类型
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典标签
     */
    @TableField("dict_label")
    private String dictLabel;

    /**
     * 字典值
     */
    @TableField("dict_value")
    private String dictValue;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 状态[0:禁用，1:启用]
     */
    @TableField("status")
    private Integer status;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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

    /**
     * 是否删除[0:未删除，1:已删除]
     */
    @TableField("is_del")
    @TableLogic
    private Boolean deleted;

}