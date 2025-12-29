package io.charqe.qs.model.message.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息表
 * </p>
 *
 * @author joy
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 发送者ID
     */
    private Integer senderId;

    /**
     * 接收者ID
     */
    private Integer receiverId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型[1:系统通知，2:评论回复，3:私信]
     */
    private Integer type;

    /**
     * 是否已读[0:未读，1:已读]
     */
    private Boolean readStatus;

    /**
     * 关联ID（如评论ID、文章ID等）
     */
    private Integer relatedId;

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
     * 是否删除[0:未删除，1:已删除]
     */
    @TableLogic
    private Boolean deleted;
}
