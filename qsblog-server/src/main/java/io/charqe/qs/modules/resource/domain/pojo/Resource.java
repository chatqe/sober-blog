package io.charqe.qs.modules.resource.domain.pojo;

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
@TableName("resource")
public class Resource extends BaseDO {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 资源类型
     */
    @TableField("type")
    private String type;

    /**
     * 是否启用[0:否，1:是]
     */
    @TableField("status")
    private Boolean status;

    /**
     * 存储平台
     */
    @TableField("store_type")
    private String storeType;

    /**
     * 资源路径
     */
    @TableField("path")
    private String path;

    /**
     * 资源内容的大小，单位：字节
     */
    @TableField("size")
    private Integer size;

    /**
     * 资源的 MIME 类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件名称
     */
    @TableField("original_name")
    private String originalName;

}
