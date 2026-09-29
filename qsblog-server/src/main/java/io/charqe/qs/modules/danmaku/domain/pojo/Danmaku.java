package io.charqe.qs.modules.danmaku.domain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**geng
 * *
 * 
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("danmaku")
public class Danmaku extends BaseDO {

    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 留言
     */
    @TableField("message")
    private String message;

    /**
     * 创建人
     */
    @TableField("create_by")
    private String createBy;

}
