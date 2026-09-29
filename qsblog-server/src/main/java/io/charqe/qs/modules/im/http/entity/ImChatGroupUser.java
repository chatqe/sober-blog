package io.charqe.qs.modules.im.http.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("im_chat_group_user")
public class ImChatGroupUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 群ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 审核用户ID
     */
    @TableField("verify_user_id")
    private Long verifyUserId;

    /**
     * 是否管理员[0:否，1:是]
     */
    @TableField("admin_flag")
    private Boolean adminFlag;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 用户状态[0:未审核，1:审核通过，2:禁言]
     */
    @TableField("user_status")
    private Integer userStatus;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;


}


