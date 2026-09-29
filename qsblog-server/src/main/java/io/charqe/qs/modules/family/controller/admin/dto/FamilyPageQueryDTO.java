package io.charqe.qs.modules.family.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 家庭卡片分页查询参数
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "家庭卡片分页查询参数")
public class FamilyPageQueryDTO {

    @Schema(description = "状态：0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "页码", example = "1")
    private int pageNum = 1;

    @Schema(description = "每页条数", example = "10")
    private int pageSize = 10;
}
