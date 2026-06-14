package io.charqe.qs.modules.moment.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@TableName("moment")
public class Moment extends BaseDO {

    /**
     * 发布用户ID（关联用户表主键）
     */
    private Long userId;

    /**
     * 动态内容
     */
    private String content;

    /**
     * 动态类型：friend-朋友圈 news-新闻动态
     */
    private String type;

    /**
     * 来源ID（如新闻动态关联的文章ID）
     */
    private Long source;

    /**
     * 图片/视频等文件JSON
     */
    private String fileJson;

    /**
     * 点赞数
     */
    private Integer likes;

    /**
     * 是否公开 0-私密 1-公开
     */
    private Integer isPublic;

    /**
     * 动态状态 0-草稿 1-正常 2-审核中 3-违规下架
     */
    private Integer status;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 修改人
     */
    private String updateBy;

    /**
     * 删除时间（NULL表示未删除）
     */
    private LocalDateTime delTime;

}
