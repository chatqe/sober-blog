package io.charqe.qs.modules.behavior.enums;

import io.charqe.qs.common.enums.BaseEnum;

import java.util.Arrays;

/**
 * 行为类型枚举
 *
 * @author Joy
 * @since 2026-01-19 17:07
 */
public enum BehActionType implements BaseEnum<Integer> {
    /**
     * 点赞操作
     */
    LIKE(1, "点赞"),

    /**
     * 点踩操作
     */
    DISLIKE(2, "点踩"),

    /**
     * 订阅操作
     */
    SUBSCRIBE(3, "订阅"),
    /**
     * 转发操作
     */
    FORWARD(4, "转发"),
    /**
     * 评论操作
     */
    COMMENT(5, "评论"),


    /**
     * 取消点赞操作
     */
    UN_LIKE(6, "取消点赞"),

    /**
     * 取消点踩操作
     */
    UN_DISLIKE(7, "取消点踩"),
    /**
     * 取消订阅操作
     */
    UN_SUBSCRIBE(8, "取消订阅"),

    /**
     * 转发操作
     */
    UN_FORWARD(9, "取消转发"),
    /**
     * 评论操作
     */
    UN_COMMENT(10, "取消评论"),
    /**
     * 浏览操作
     */
    VIEW(0, "浏览"),
    ;

    // 操作编码
    private final Integer code;
    // 操作描述
    private final String desc;

    /**
     * 枚举构造方法
     *
     * @param code 操作编码
     * @param desc 操作描述
     */
    BehActionType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举实例
     *
     * @param code 操作编码
     * @return 对应的枚举实例
     */
    public static BehActionType getByCode(int code) {
        for (BehActionType b : BehActionType.values()) {
            if (b.code().equals(code)) {
                return b;
            }
        }
        // 抛出异常或返回null，这里选择抛出异常更符合严谨性
        throw new IllegalArgumentException("无效的操作类型编码: " + code);
    }

    public static Integer codeByName(String name) {
        for (BehActionType b : BehActionType.values()) {
            if (b.name().equals(name)) {
                return b.code();
            }
        }
        // 抛出异常或返回null，这里选择抛出异常更符合严谨性
        throw new IllegalArgumentException("无效的操作类型名称: " + name);
    }

    /**
     * 判断是否存在该编码的操作类型
     *
     * @param code 操作编码
     * @return true-存在，false-不存在
     */
    public static boolean contains(Integer code) {
        return Arrays.stream(values())
                .anyMatch(e -> e.code().equals(code));
    }

    @Override
    public Integer code() {
        return code;
    }

    @Override
    public String desc() {
        return desc;
    }
}
