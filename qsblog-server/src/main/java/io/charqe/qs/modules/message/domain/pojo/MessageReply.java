package io.charqe.qs.modules.message.domain.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 留言回复实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message_reply")
public class MessageReply extends BaseDO {

    /** 主留言ID */
    @TableField("message_id")
    private Long messageId;

    /** 直接父回复ID，0=回复主留言 */
    @TableField("parent_id")
    private Long parentId;

    /** 顶级回复ID，0=自身为根 */
    @TableField("root_id")
    private Long rootId;

    /** 被回复的回复ID，0=回复主留言 */
    @TableField("reply_to_id")
    private Long replyToId;

    /** 被回复人昵称，冗余展示 */
    @TableField("reply_to_name")
    private String replyToName;

    /** 回复用户ID，0=游客 */
    @TableField("user_id")
    private Long userId;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像URL */
    @TableField("avatar")
    private String avatar;

    /** 回复者邮箱，必填，不对外展示 */
    @TableField("email")
    private String email;

    /** 邮箱SHA256，防刷/去重 */
    @TableField("email_hash")
    private String emailHash;

    /** 个人网址，选填 */
    @TableField("website")
    private String website;

    /** 回复内容 */
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

    /** 直接子回复数 */
    @TableField("child_cnt")
    private Integer childCnt;

    /** 0=否 1=管理员/站长回复 */
    @TableField("is_admin")
    private Integer isAdmin;

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
