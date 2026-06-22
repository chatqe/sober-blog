package io.charqe.qs.modules.category.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分类列表响应VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "分类列表VO")
public class CategoryListVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类描述")
    private String desc;

    @Schema(description = "分类类型")
    private Integer type;

    @Schema(description = "是否展示")
    private Integer isShow;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "创建时间")
    private String createTime;
}
