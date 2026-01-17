package io.charqe.qs.modules.sysconfig.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
public class SysConfig extends BaseDO {

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

