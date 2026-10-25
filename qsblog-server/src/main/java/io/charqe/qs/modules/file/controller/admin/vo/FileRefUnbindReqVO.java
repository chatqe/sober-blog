package io.charqe.qs.modules.file.controller.admin.vo;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 文件引用解绑入参：定位维度与绑定一致，重复解绑视为幂等成功。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileRefUnbindReqVO {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    @NotNull(message = "业务类型不能为空")
    @Min(value = 1, message = "业务类型不合法")
    private Integer bizType;

    @NotNull(message = "业务ID不能为空")
    @Min(value = 1, message = "业务ID不合法")
    private Long bizId;

    @Pattern(regexp = "^[A-Za-z0-9_-]{0,32}$", message = "业务字段只能包含字母、数字、下划线或短横线，且不超过 32 位")
    private String bizField;
}
