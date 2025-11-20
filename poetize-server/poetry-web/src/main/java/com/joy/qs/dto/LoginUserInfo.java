package com.joy.qs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用用户对象 包含用户基本信息
 * 后期在各个微服务中都有可能使用 所以放到公共包下
 * @author Upjqs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserInfo {
    private Long  id;
}
