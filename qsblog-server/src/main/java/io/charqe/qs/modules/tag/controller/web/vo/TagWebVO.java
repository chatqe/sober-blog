package io.charqe.qs.modules.tag.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标签公开VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "标签公开VO")
public class TagWebVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签描述")
    private String desc;
}
