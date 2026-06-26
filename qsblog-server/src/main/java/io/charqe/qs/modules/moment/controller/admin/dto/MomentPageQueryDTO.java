package io.charqe.qs.modules.moment.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 动态分页查询参数（Admin）
 *
 * @author Joy
 * @since 2026-06-25
 */
@Data
@Schema(description = "动态分页查询参数")
public class MomentPageQueryDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "状态: 0-草稿 1-正常 2-审核中 3-违规下架")
    private Integer status;

    @Schema(description = "可见性: 0-私密 1-好友 2-公开")
    private Integer visibility;

    @Schema(description = "来源: 1-本站 2-微信 3-QQ 4-微博 5-抖音")
    private Integer source;

    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private int pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private int pageSize = 10;
}
