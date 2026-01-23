package io.charqe.qs.modules.user.domain.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.charqe.qs.common.model.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user")
public class User extends BaseDO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */

    private String password;

    /**
     * 手机号
     */

    private String phoneNumber;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 是否启用[0:否，1:是]
     */
    private Boolean userStatus;

    /**
     * 性别[1:男，2:女，0:保密]
     */
    private Integer gender;

    /**
     * openId
     */
    private String openId;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 赞赏
     */
    private String admire;

    /**
     * 订阅
     */
    private String subscribe;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 用户类型[0:admin，1:管理员，2:普通用户]
     */
    private Integer userType;

    /**
     * 最终修改人
     */
    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

}
