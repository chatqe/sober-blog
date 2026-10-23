package io.charqe.qs.modules.danmaku.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台弹幕展示VO
 *
 * @author Joy
 * @since 2026-10-04 22:56
 */
@Data
public class AdminDanmakuVO {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "用户头像")
    private String avatar;


    @Schema(description = "弹幕消息")
    private String message;

    @Schema(description = "是否删除")
    private Integer isDel;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
