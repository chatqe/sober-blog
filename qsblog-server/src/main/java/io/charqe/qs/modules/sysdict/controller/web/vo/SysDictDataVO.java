package io.charqe.qs.modules.sysdict.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典数据VO（web端返回）
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "字典数据VO")
public class SysDictDataVO {

    @Schema(description = "字典项编码", example = "M")
    private String itemCode;

    @Schema(description = "字典项实际存储值", example = "0")
    private String itemValue;

    @Schema(description = "字典项显示标签", example = "男")
    private String itemLabel;

    @Schema(description = "排序值", example = "1")
    private Integer sort;
}
