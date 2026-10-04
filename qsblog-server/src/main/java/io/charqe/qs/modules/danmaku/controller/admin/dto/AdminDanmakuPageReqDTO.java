package io.charqe.qs.modules.danmaku.controller.admin.dto;

import io.charqe.qs.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * admin弹幕分页请求DTO
 *
 * @author Joy
 * @since 2026-10-04 22:07
 */
@Data
public class AdminDanmakuPageReqDTO extends PageQuery {

    /**
     * 模糊搜索关键字
     */
    @Schema(description = "模糊搜索关键字")
    private String keyword;
    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

}
