package io.charqe.qs.modules.user.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.*;

/** 后台用户编辑白名单；不接受密码、操作者身份等字段。 */
@Data
@Schema(description = "后台用户编辑请求，未传字段保持不变，空字符串清空可选资料")
public class AdminUserUpdateReq {
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID无效")
    private Long id;

    @Size(min = 1, max = 32, message = "用户名长度须为1至32个字符")
    private String username;

    @Schema(description = "手机号，未传保持不变，空字符串清除")
    @Pattern(regexp = "^(?:1[3-9][0-9]{9})?$", message = "请输入有效的11位手机号")
    private String phoneNumber;

    @Schema(description = "邮箱，未传保持不变，空字符串清除")
    @Size(max = 32, message = "邮箱不能超过32个字符")
    @Email(message = "请输入有效的邮箱地址")
    @Pattern(regexp = "^(?:[^\\s@]+@[^\\s@]+\\.[^\\s@]+)?$", message = "请输入有效的邮箱地址")
    private String email;

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    @Size(max = 256, message = "头像地址不能超过256个字符")
    private String avatar;

    @Min(0)
    @Max(2)
    private Integer gender;

    @Size(max = 4096, message = "简介不能超过4096个字符")
    private String introduction;

    @Size(max = 32, message = "赞赏信息不能超过32个字符")
    private String admire;

    @Schema(description = "用户状态：0-禁用，1-启用", allowableValues = {"0", "1"})
    @Min(0)
    @Max(1)
    private Integer userStatus;

    @Schema(description = "用户角色：0-Boss，1-Admin，2-Normal", allowableValues = {"0", "1", "2"})
    @Min(0)
    @Max(2)
    private Integer userType;
}
