package io.charqe.qs.modules.category.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 分类新增/编辑请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "分类请求")
public class CategorySaveReqDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", required = true, example = "技术分享")
    private String name;

    @Schema(description = "分类描述", example = "技术类文章分类")
    private String desc;

    @Schema(description = "分类类型: 0-导航栏分类, 1-普通分类", example = "1")
    private Integer type;

    @Schema(description = "启用状态: 0-停用 1-启用。不传则新增走库列默认值 1、编辑保持原值不变；"
            + "停用的分类不进公开口径 options，后台下拉置灰但存量文章仍可读到", example = "1")
    private Integer status;

    @Schema(description = "是否展示: 0-否, 1-是", example = "1")
    private Integer isShow;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "图标", example = "https://example.com/icon.png")
    private String icon;
}
