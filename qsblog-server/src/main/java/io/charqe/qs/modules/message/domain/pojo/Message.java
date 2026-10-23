package io.charqe.qs.modules.message.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 留言主表实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message")
public class Message extends BaseDO {

    /** 留言用户ID，0=游客 */
    @TableField("user_id")
    private Long userId;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像URL */
    @TableField("avatar")
    private String avatar;

    /** 邮箱，必填，不对外展示 */
    @TableField("email")
    private String email;

    /** 邮箱SHA256，防刷/去重 */
    @TableField("email_hash")
    private String emailHash;

    /** 个人网址，选填 */
    @TableField("website")
    private String website;

    /** 留言内容 */
    @TableField("content")
    private String content;

    /** 内容SHA256，防刷 */
    @TableField("content_hash")
    private String contentHash;

    /** IP地址 */
    @TableField("ip")
    private String ip;

    /** User-Agent原文 */
    @TableField("ua")
    private String ua;

    /** IP完整地理位置，页面展示如：香港 */
    @TableField("ip_location")
    private String ipLocation;

    /** 国家 */
    @TableField("country")
    private String country;

    /** 省份 */
    @TableField("province")
    private String province;

    /** 城市 */
    @TableField("city")
    private String city;

    /** 区县 */
    @TableField("district")
    private String district;

    /** 运营商 */
    @TableField("isp")
    private String isp;

    /** 操作系统（含版本），如 Windows 10、Windows 11 */
    @TableField("os")
    private String os;

    /** 浏览器（含版本），如 Chrome 154.0.0.0 */
    @TableField("browser")
    private String browser;

    /** 设备类型：PC/Mobile/Pad */
    @TableField("device_type")
    private String deviceType;

    /** 点赞数 */
    @TableField("like_count")
    private Integer likeCount;

    /** 踩数 */
    @TableField("dislike_count")
    private Integer dislikeCount;

    /** 回复总数 */
    @TableField("reply_count")
    private Integer replyCount;

    /** 最后回复时间 */
    @TableField("last_reply_time")
    private java.time.LocalDateTime lastReplyTime;

    /** 0=否 1=置顶 */
    @TableField("is_top")
    private Integer isTop;

    /** 1=正常 2=审核中 3=拒绝 4=隐藏 */
    @TableField("status")
    private Integer status;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    /** 审核人ID */
    @TableField("moderator_id")
    private Long moderatorId;

    /** 审核时间 */
    @TableField("moderate_time")
    private java.time.LocalDateTime moderateTime;
}
