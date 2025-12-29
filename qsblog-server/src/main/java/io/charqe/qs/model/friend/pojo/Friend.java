package io.charqe.qs.model.friend.pojo;

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
@TableName("friend")
public class Friend implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 友链名称
     */
    private String name;

    /**
     * 友链地址
     */
    private String url;

    /**
     * 友链头像
     */
    private String avatar;

    /**
     * 友链描述
     */
    private String description;

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
