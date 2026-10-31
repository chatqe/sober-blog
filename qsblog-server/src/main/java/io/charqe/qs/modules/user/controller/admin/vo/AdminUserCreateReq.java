package io.charqe.qs.modules.user.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;

/** 后台新增用户请求；不接受操作者身份等字段，密码为前端 AES 加密后的密文。 */
@Data
@Schema(description = "后台新增用户请求")
public class AdminUserCreateReq {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 32, message = "用户名长度须为1至32个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码密文（AES-ECB，与登录/注册同口径），服务端解密后 BCrypt 落库")
    private String password;

    @Schema(description = "用户角色：0-Boss，1-Admin，2-Normal，缺省为 2；Admin 操作者不可创建 Boss")
    @Min(0)
    @Max(2)
    private Integer userType;

    @Schema(description = "用户状态：0-禁用，1-启用，缺省为 1")
    @Min(0)
    @Max(1)
    private Integer userStatus;

    @Schema(description = "手机号")
    @Pattern(regexp = "^(?:1[3-9][0-9]{9})?$", message = "请输入有效的11位手机号")
    private String phoneNumber;

    @Schema(description = "邮箱")
    @Size(max = 32, message = "邮箱不能超过32个字符")
    @Email(message = "请输入有效的邮箱地址")
    @Pattern(regexp = "^(?:[^\\s@]+@[^\\s@]+\\.[^\\s@]+)?$", message = "请输入有效的邮箱地址")
    private String email;

    @Size(max = 256, message = "头像地址不能超过256个字符")
    private String avatar;

    @Min(0)
    @Max(2)
    private Integer gender;

    @Size(max = 4096, message = "简介不能超过4096个字符")
    private String introduction;

    @Size(max = 32, message = "赞赏信息不能超过32个字符")
    private String admire;

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
