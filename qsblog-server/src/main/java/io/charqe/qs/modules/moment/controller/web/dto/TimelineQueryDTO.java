package io.charqe.qs.modules.moment.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 动态分页查询参数（Web端）
 */
@Data
@Schema(description = "动态分页查询参数")
public class TimelineQueryDTO {

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 50, message = "每页条数最大为50")
    private int pageSize = 10;
}
