package io.charqe.qs.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 可见性枚举
 *
 * @author system
 * @since 2026-01-17
 */
public enum VisibilityEnum implements BaseEnum<Integer> {
    /**
     * 仅自己可见
     */
    PRIVATE(0, "仅自己可见"),
    
    /**
     * 所有人可见
     */
    PUBLIC(1, "所有人可见");
    
    private final int code;
    private final String desc;
    
    VisibilityEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    @Override
    public Integer code() {
        return code;
    }
    
    @Override
    public String desc() {
        return desc;
    }
    
    /**
     * 根据编码获取枚举实例
     *
     * @param code 编码
     * @return 枚举实例，若不存在则返回null
     */
    public static VisibilityEnum getByCode(Integer code) {
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