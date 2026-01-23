package io.charqe.qs.modules.sysdict.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型表
 *
 * @author Joy
 * @since 2026-01-16 22:08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_dict_type")
public class SysDictType extends BaseDO {



    /**
     * 字典类型
     */
    @TableField("dict_type")
    private String dictType;

    /**
     * 字典名称
     */
    @TableField("dict_name")
    private String dictName;

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





}