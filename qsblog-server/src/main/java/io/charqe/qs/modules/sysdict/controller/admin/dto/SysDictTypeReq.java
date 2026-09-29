package io.charqe.qs.modules.sysdict.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 字典类型新增/编辑请求
 *
 * @author Joy
 * @since 2026-06-22
 */
@Data
@Schema(description = "字典类型请求")
public class SysDictTypeReq {

    @Schema(description = "字典类型ID（编辑时传入）")
    private Long id;

    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码", example = "gender")
    private String typeCode;

    @NotBlank(message = "字典类型名称不能为空")
    @Schema(description = "字典类型名称", example = "性别")
    private String typeName;

    @Schema(description = "状态：0-禁用, 1-启用", example = "1")
    private Integer status;

    @Schema(description = "排序权重", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "用户性别字典")
    private String remark;
}
