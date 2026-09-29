package io.charqe.qs.modules.sysdict.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 字典数据新增/编辑请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "字典数据请求")
public class SysDictDataReq {

    @Schema(description = "字典数据ID（编辑时传入）")
    private Long id;

    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码", example = "gender")
    private String typeCode;

    @NotBlank(message = "字典项编码不能为空")
    @Schema(description = "字典项编码", example = "M")
    private String itemCode;

    @Schema(description = "字典项实际存储值", example = "0")
    private String itemValue;

    @NotBlank(message = "字典项标签不能为空")
    @Schema(description = "字典项显示标签", example = "男")
    private String itemLabel;

    @Schema(description = "排序值", example = "1")
    private Integer sort;

    @Schema(description = "状态：0-禁用, 1-启用", example = "1")
    private Integer status;

    @Schema(description = "是否默认值：0-否, 1-是", example = "0")
    private Integer isDefault;

    @Schema(description = "备注", example = "男性")
    private String remark;
}
