package io.charqe.qs.modules.comment.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("comment")
public class Comment extends BaseDO {

    /**
     * 父评论ID，0表示根评论
     */
    private Long parentId;

    /**
     * 顶级祖先评论ID，0表示自身就是根评论
     */
    private Long rootId;

    /**
     * 业务类型：1文章 2评论 3视频
     */
    private Integer bizType;

    /**
     * 对应业务的主键ID
     */
    private Long bizId;

    /**
     * 评论人ID
     */
    private Long authorId;

    /**
     * 评论人名称（冗余字段）
     */
    private String authorName;

    /**
     * 评论人头像（冗余字段）
     */
    private String authorAvatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 回复的目标用户ID，0表示无特定回复对象
     */
    private Long replyToUserId;

    /**
     * 评论状态：1=正常 2=禁言 3=审核中
     */
    private Integer status;

    /**
     * 评论提交时的IP地址
     */
    private String ip;

    /**
     * 评论提交时的用户代理
     */
    private String ua;

    /**
     * 是否为内容作者：0=否 1=是
     */
    private Integer isOwner;

    /**
     * 是否置顶：0=否 1=是
     */
    private Integer isTop;

    /**
     * 评论点赞数
     */
    private Integer likeCnt;

    /**
     * 直接子评论数量
     */
    private Integer childCnt;

    /**
     * 内容SHA256，防刷评
     */
    private String contentHash;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 审核人ID
     */
    private Long moderatorId;

    /**
     * 审核时间
     */
    private LocalDateTime moderateTime;

}
