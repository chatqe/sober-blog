package io.charqe.qs.modules.danmaku.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 弹幕列表VO
 *
 * @author Joy
 * @since 2026-10-03 5:04
 */
@Data
public class DanmakuVO {

    /**
     * 用户头像
     */
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 弹幕消息
     */
    @Schema(description = "弹幕消息")
    private String message;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
