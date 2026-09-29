package io.charqe.qs.modules.webInfo.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 首页统计信息聚合VO
 *
 * @author Joy
 * @since 2026-09-07
 */
@Data
@Schema(description = "首页统计信息")
public class HomeStatsVO {

    @Schema(description = "文章总数")
    private Long articleTotal;

    @Schema(description = "分类总数")
    private Long categoryTotal;

    @Schema(description = "分类列表（含文章计数）")
    private List<CategoryStatsVO> categories;

    /**
     * 单个分类统计VO
     */
    @Data
    @Schema(description = "分类统计")
    public static class CategoryStatsVO {
        @Schema(description = "分类ID")
        private Long id;

        @Schema(description = "分类名称")
        private String name;

        @Schema(description = "分类描述")
        private String desc;

        @Schema(description = "分类类型: 0-导航栏分类, 1-普通分类")
        private Integer type;

        @Schema(description = "排序值")
        private Integer sort;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "该分类下的文章数量")
        private Long countOfSort;
    }
}
