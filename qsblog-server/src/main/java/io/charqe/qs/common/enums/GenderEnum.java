package io.charqe.qs.common.enums;

import java.util.Arrays;
import java.util.Objects;

/**
 * 用户性别枚举
 *
 * @author system
 * @since 2026-01-17
 */
public enum GenderEnum implements BaseEnum<Integer> {
    /**
     * 保密性别
     */
    NONE(0, "保密"),
    
    /**
     * 男性
     */
    BOY(1, "男"),
    
    /**
     * 女性
     */
    GIRL(2, "女");
    
    private final int code;
    private final String desc;
    
    GenderEnum(int code, String desc) {
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
    public static GenderEnum getByCode(Integer code) {
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