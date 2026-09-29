package io.charqe.qs.modules.user.controller.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用用户对象 包含用户基本信息
 * 后期在各个微服务中都有可能使用 所以放到公共包下
 *
 * @author Upjqs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserInfo {
    private Long id;

    private String username;

    /**
     * 用户类型 0:admin，1:管理员，2:普通用户
     */
    private Integer userType;

    private String avatar;

    // private List roles;
}


