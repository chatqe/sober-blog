package io.charqe.qs.modules.sysdict.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Common字典数据VO（web端返回）
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "通用字典数据VO")
public class DictVO {

    @Schema(description = "字典项实际存储值", example = "0")
    private String value;

    @Schema(description = "字典项显示标签", example = "男")
    private String label;

    @Schema(description = "排序值", example = "1")
    private Integer sort;
}
