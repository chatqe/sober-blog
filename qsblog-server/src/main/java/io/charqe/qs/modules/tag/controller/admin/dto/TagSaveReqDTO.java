package io.charqe.qs.modules.tag.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 标签新增/编辑请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "标签请求")
public class TagSaveReqDTO {

    @Schema(description = "ID（编辑时传入）")
    private Long id;

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "所属分类ID", required = true, example = "1")
    private Long categoryId;

    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", required = true, example = "Java")
    private String name;

    @Schema(description = "标签描述", example = "Java编程语言相关文章")
    private String desc;
}
