package io.charqe.qs.modules.moment.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 动态文件关联实体
 * 通过 file_id 关联 fileInfo 表，支撑混合内容（图文/视频/音频）
 *
 * @author Joy
 * @since 2026-06-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("moment_media")
public class MomentMedia {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 动态ID
     */
    @TableField("moment_id")
    private Long momentId;

    /**
     * 关联fileInfo表主键
     */
    @TableField("file_id")
    private Long fileId;

    /**
     * 媒体类型: 1-图片 2-视频 3-音频
     */
    @TableField("media_type")
    private Integer mediaType;

    /**
     * 排序权重（数值越小越靠前）
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;
}
