package io.charqe.qs.modules.weblinks.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 友链表
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("weblinks")
public class Weblinks implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 标题
     */
    private String title;
    /**
     * 分类
     */
    private String classify;

    /**
     * 友链地址
     */
    private String url;

    /**
     * 友链头像
     */
    private String cover;

    /**
     * 友链描述
     */
    private String desc;
    /**
     * 备注
     */
    private String remark;

    /**
     * 资源类型
     */
    private String type;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态[0:未审核，1:已审核]
     */
    private Boolean status;

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
