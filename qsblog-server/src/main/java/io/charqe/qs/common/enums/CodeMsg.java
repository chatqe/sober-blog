package io.charqe.qs.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * API响应码枚举
 *
 * @author system
 * @since 2026-01-17
 */
public enum CodeMsg {
    /**
     * 成功
     */
    SUCCESS(200, "成功"),

    /**
     * 参数异常
     */
    PARAMETER_ERROR(400, "参数异常"),

    /**
     * 未登录
     */
    NOT_LOGIN(300, "未登录，请登录后再进行操作"),

    /**
     * 登录已过期
     */
    LOGIN_EXPIRED(301, "登录已过期，请重新登录"),

    /**
     * 系统维护中
     */
    SYSTEM_REPAIR(302, "系统维护中，敬请期待"),

    /**
     * 服务异常
     */
    FAIL(500, "服务异常");

    private final int code;
    private final String msg;

    CodeMsg(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public Integer getCode() {
        return code;
    }


    public String getMsg() {
        return msg;
    }

    /**
     * 根据编码获取枚举实例
     *
     * @param code 编码
     * @return 枚举实例，若不存在则返回null
     */
    public static CodeMsg getByCode(Integer code) {
        if (Objects.isNull(code)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(enumItem -> enumItem.code == code)
                .findFirst()
                .orElse(null);
    }

    /**
     * 验证编码是否有效
     *
     * @param code 编码
     * @return true 有效，false 无效
     */
    public static boolean isValid(Integer code) {
        return getByCode(code) != null;
    }
}


