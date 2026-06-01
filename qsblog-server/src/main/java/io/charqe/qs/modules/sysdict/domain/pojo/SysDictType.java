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
     * 字典类型编码，全局唯一, 如 "gender"
     */
    private String typeCode;

    /**
     * 字典类型名称, 用于展示，如 "性别"
     */
    private String typeName;

    /**
     * 状态：0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 排序权重，值越小越靠前
     */
    private Integer sort;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;





}