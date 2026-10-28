package io.charqe.qs.modules.file.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 文件元信息编辑请求vo
 * <p>
 * 仅允许改原始文件名与业务场景，字段留空表示保持原值；存储路径、MIME、归属等
 * 由上传链路决定，不开放编辑。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileUpdateReqVO {

    @NotNull(message = "文件ID不能为空")
    @Schema(description = "文件ID")
    private Long id;

    @Size(max = 255, message = "文件名长度不能超过 255")
    @Schema(description = "原始文件名，留空表示不修改")
    private String originalName;

    @Schema(description = "业务场景，留空表示不修改")
    private String scene;
}
