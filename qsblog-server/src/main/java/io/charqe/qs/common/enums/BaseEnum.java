package io.charqe.qs.common.enums;

import java.util.Objects;

/**
 * 业务枚举基类接口，定义所有枚举的通用方法
 *
 * @author system
 * @since 2026-01-17
 */
public interface BaseEnum<T> {
    /**
     * 枚举编码
     *
     * @return 枚举编码
     */
    T getCode();

    /**
     * 枚举描述
     *
     * @return 枚举描述
     */
    String getDesc();

    /**
     * 判断编码是否匹配
     */
    default boolean match(T targetCode) {
        return Objects.equals(getCode(), targetCode);
    }
}