package io.charqe.qs.modules.category.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分类公开VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "分类公开VO")
public class CategoryWebVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类描述")
    private String desc;

    // @Schema(description = "分类类型: 0-导航栏分类, 1-普通分类")
    // private Integer type;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "图标")
    private String icon;
}
