package io.charqe.qs.modules.moment.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 三方同步记录实体
 * 职责解耦：出向同步（本站→微信/微博/QQ）和入向同步（三方→本站）
 *
 * @author Joy
 * @since 2026-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("moment_social_sync")
public class MomentSocialSync {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 本地动态ID
     */
    @TableField("moment_id")
    private Long momentId;

    /**
     * 同步方向: 1-出向(本站发往三方) 2-入向(三方拉取至本站)
     */
    @TableField("direction")
    private Integer direction;

    /**
     * 平台标识: wechat | qq | weibo | douyin
     */
    @TableField("platform")
    private String platform;

    /**
     * 三方平台返回的发布ID（用于删除/修改/幂等）
     */
    @TableField("external_id")
    private String externalId;

    /**
     * 三方平台的内容链接
     */
    @TableField("external_url")
    private String externalUrl;

    /**
     * 同步状态: 0-待同步 1-同步中 2-成功 3-失败
     */
    @TableField("sync_status")
    private Integer syncStatus;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 失败时的报错信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 请求三方的原始入参（方便排查问题）
     */
    @TableField("sync_data")
    private String syncData;

    /**
     * 同步完成时间
     */
    @TableField("synced_at")
    private LocalDateTime syncedAt;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
