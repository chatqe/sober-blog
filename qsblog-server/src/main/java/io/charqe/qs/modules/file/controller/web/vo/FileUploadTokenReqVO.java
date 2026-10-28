package io.charqe.qs.modules.file.controller.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 上传凭证请求：只传文件元数据，Key 由服务端生成。
 *
 * @author Joy
 * @since 2026-10-26
 */
@Data
public class FileUploadTokenReqVO {

    @Schema(description = "业务场景：avatar/moment/comment/article-cover/article-content/system", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "业务场景不能为空")
    private String scene;

    @Schema(description = "原始文件名，用于推导扩展名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件名不能为空")
    private String originalName;

    @Schema(description = "MIME 类型，如 image/png", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "MIME 类型不能为空")
    private String mimeType;

    @Schema(description = "文件大小（字节）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于 0")
    private Long fileSize;
}
