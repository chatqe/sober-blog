package io.charqe.qs.modules.tag.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 标签详情响应VO
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "标签详情VO")
public class TagDetailVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签描述")
    private String desc;

    @Schema(description = "排序权重：数字小的在前面")
    private Integer sort;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "更新时间")
    private String updateTime;
}
