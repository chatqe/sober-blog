package io.charqe.qs.common.enums;

/**
 * 枚举基类接口，定义所有枚举的通用方法
 *
 * @author system
 * @since 2026-01-17
 */
public interface BaseEnum<T> {
    /**
     * 获取枚举编码
     *
     * @return 枚举编码
     */
    T getCode();
    
    /**
     * 获取枚举描述
     *
     * @return 枚举描述
     */
    String getDesc();


}