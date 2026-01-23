package io.charqe.qs.modules.sysconfig.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_config")
public class SysConfig  {
    /**
     * 主键（自增）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * 键名
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 键值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 1 私用 2 公开
     */
    @TableField("config_type")
    private String configType;

}

