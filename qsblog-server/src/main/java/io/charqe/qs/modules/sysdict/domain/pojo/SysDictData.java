package io.charqe.qs.modules.sysdict.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 字典类型编码，关联 sys_dict_type.type_code
     */
    private String typeCode;

    /**
     * 字典项编码，同一 type_code 下唯一，如 "M"
     */
    private String itemCode;

    /**
     * 字典项实际存储值，如 0/1 或具体代码值
     */
    private String itemValue;

    /**
     * 字典项显示标签，如 "男"
     */
    private String itemLabel;

    /**
     * 排序值，值越小越靠前
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 是否默认值：0-否, 1-是
     */
    private Integer isDefault;

    /**
     * 创建人标识
     */
    private String createBy;

    /**
     * 更新人标识
     */
    private String updateBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    @TableField("is_del")
    private Integer isDel;

}