package io.charqe.qs.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 通用状态枚举
 *
 * @author system
 * @since 2026-01-17
 */
public enum StatusEnum implements BaseEnum<Integer> {
    /**
     * 禁用状态
     */
    DISABLE(0, "禁用"),
    
    /**
     * 启用状态
     */
    ENABLE(1, "启用");
    
    private final int code;
    private final String desc;
    
    StatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    @Override
    public Integer getCode() {
        return code;
    }
    
    @Override
    public String getDesc() {
        return desc;
    }
    
    /**
     * 根据编码获取枚举实例
     *
     * @param code 编码
     * @return 枚举实例，若不存在则返回null
     */
    public static StatusEnum getByCode(Integer code) {
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