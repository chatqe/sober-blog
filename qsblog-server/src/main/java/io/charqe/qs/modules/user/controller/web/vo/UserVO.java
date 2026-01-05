package io.charqe.qs.modules.user.controller.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@Schema(name = "UserVO", description = "用户信息数据模型")
public class UserVO {

    @Schema(name = "id", description = "用户ID", example = "1")
    private Integer id;

    @Schema(name = "username", description = "用户名", required = true, example = "testuser")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(name = "phoneNumber", description = "手机号码", example = "13800138000")
    private String phoneNumber;

    @Schema(name = "email", description = "邮箱地址", example = "test@example.com")
    private String email;

    @Schema(name = "password", description = "密码", required = true, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(name = "gender", description = "性别，0-未知，1-男，2-女", allowableValues = {"0", "1", "2"}, example = "1")
    private Integer gender;

    @Schema(name = "avatar", description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(name = "introduction", description = "个人简介", example = "这是一个测试用户")
    private String introduction;

    @Schema(name = "subscribe", description = "订阅的标签ID列表，以逗号分隔", example = "1,2,3")
    private String subscribe;

    @Schema(name = "createTime", description = "创建时间", format = "date-time", example = "2023-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(name = "updateTime", description = "更新时间", format = "date-time", example = "2023-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(name = "updateBy", description = "更新人", example = "admin")
    private String updateBy;

    @Schema(name = "isBoss", description = "是否为管理员", defaultValue = "false", example = "false")
    private Boolean isBoss = false;

    @Schema(name = "accessToken", description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(name = "code", description = "验证码", example = "123456")
    private String code;
}


