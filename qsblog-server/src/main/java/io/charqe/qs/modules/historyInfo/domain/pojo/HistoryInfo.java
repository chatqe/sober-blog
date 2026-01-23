package io.charqe.qs.modules.historyInfo.domain.pojo;

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
@TableName("history_info")
public class HistoryInfo  {
    /**
     * 主键（自增）
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * ip
     */
    @TableField("ip")
    private String ip;

    /**
     * 国家
     */
    @TableField("nation")
    private String nation;

    /**
     * 省份
     */
    @TableField("province")
    private String province;

    /**
     * 城市
     */
    @TableField("city")
    private String city;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}
