package io.charqe.qs.model.pageconfig.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 页面配置表
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("page_config")
public class PageConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 页面名称
     */
    private String pageName;

    /**
     * 页面标题
     */
    private String pageTitle;

    /**
     * 页面关键词
     */
    private String keywords;

    /**
     * 页面描述
     */
    private String description;

    /**
     * 页面内容
     */
    private String content;

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
     * 是否启用[0:未删除，1:已删除]
     */
    @TableLogic
    private Boolean deleted;

}
