package io.charqe.qs.modules.family.domain.pojo;

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
@TableName("family")
public class Family extends BaseDO {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 背景封面
     */
    @TableField("bg_cover")
    private String bgCover;

    /**
     * 男生头像
     */
    @TableField("man_cover")
    private String manCover;

    /**
     * 女生头像
     */
    @TableField("woman_cover")
    private String womanCover;

    /**
     * 男生昵称
     */
    @TableField("man_name")
    private String manName;

    /**
     * 女生昵称
     */
    @TableField("woman_name")
    private String womanName;

    /**
     * 计时
     */
    @TableField("timing")
    private String timing;

    /**
     * 倒计时标题
     */
    @TableField("countdown_title")
    private String countdownTitle;

    /**
     * 是否启用[0:否，1:是]
     */
    @TableField("status")
    private Boolean status;

    /**
     * 倒计时时间
     */
    @TableField("countdown_time")
    private String countdownTime;

    /**
     * 额外信息
     */
    @TableField("family_info")
    private String familyInfo;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

}
