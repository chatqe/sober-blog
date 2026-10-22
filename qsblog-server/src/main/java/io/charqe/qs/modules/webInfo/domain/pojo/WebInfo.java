package io.charqe.qs.modules.webInfo.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 网站信息表实体
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data

@TableName("web_info")
public class WebInfo  {
    /** 主键（自增） */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 网站名称 */
    @TableField("web_name")
    private String webName;

    /** 网站信息 */
    @TableField("web_title")
    private String webTitle;

    /** 公告 */
    @TableField("notices")
    private String notices;

    /** 页脚 */
    @TableField("footer")
    private String footer;

    /** 背景 */
    @TableField("background_image")
    private String backgroundImage;

    /** 头像 */
    @TableField("avatar")
    private String avatar;

    /** 随机头像 */
    @TableField("random_avatar")
    private String randomAvatar;

    /** 随机名称 */
    @TableField("random_name")
    private String randomName;

    /** 随机封面 */
    @TableField("random_cover")
    private String randomCover;

    /** 看板娘消息 */
    @TableField("waifu_json")
    private String waifuJson;
    /** 是否启用[0:否，1:是] */
    @TableField("status")
    private Boolean status;

    /** 历史总访问量 */
    @TableField(exist = false)
    private String historyAllCount;

    /** 历史当日访问量 */
    @TableField(exist = false)
    private String historyDayCount;

    /** 默认店铺类型 */
    @TableField(exist = false)
    private String defaultStoreType;
}
