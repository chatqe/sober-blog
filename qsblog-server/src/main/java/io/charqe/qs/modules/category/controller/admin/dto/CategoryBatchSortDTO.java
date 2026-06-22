package io.charqe.qs.modules.category.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量排序请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "批量排序请求")
public class CategoryBatchSortDTO {

    @Schema(description = "排序项列表", required = true)
    private List<SortItem> items;

    @Data
    public static class SortItem {
        @NotNull(message = "分类ID不能为空")
        @Schema(description = "分类ID", required = true, example = "1")
        private Long id;

        @NotNull(message = "排序值不能为空")
        @Schema(description = "排序值", required = true, example = "1")
        private Integer sort;
    }
}
