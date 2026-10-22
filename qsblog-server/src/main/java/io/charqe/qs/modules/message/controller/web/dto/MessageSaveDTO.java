package io.charqe.qs.modules.message.controller.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Schema(description = "保存留言请求体")
public class MessageSaveDTO {

    @Schema(description = "昵称", required = true, example = "访客")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最多64个字符")
    private String nickname;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "邮箱", required = true, example = "user@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Size(max = 128, message = "邮箱最多128个字符")
    private String email;

    @Schema(description = "个人网址", example = "https://example.com")
    private String website;

    @Schema(description = "留言内容", required = true, example = "这是一条测试留言")
    @NotBlank(message = "留言内容不能为空")
    @Size(max = 2000, message = "留言内容最多2000个字符")
    private String content;
}
