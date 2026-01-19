package io.charqe.qs.modules.behavior.enums;

/**
 * 行为类型枚举
 *
 * @author Joy
 * @since 2026-01-19 17:07
 */
public enum BehaviorTypeEnums {
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
     * 取消订阅操作
     */
    UNSUBSCRIBE(4, "取消订阅"),

    /**
     * 取消点赞操作
     */
    CANCEL_LIKE(5, "取消点赞"),

    /**
     * 取消点踩操作
     */
    CANCEL_DISLIKE(6, "取消点踩");

    // 操作编码
    private final int code;
    // 操作描述
    private final String desc;

    /**
     * 枚举构造方法
     * @param code 操作编码
     * @param desc 操作描述
     */
    BehaviorTypeEnums(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举实例
     * @param code 操作编码
     * @return 对应的枚举实例
     */
    public static BehaviorTypeEnums getByCode(int code) {
        for (BehaviorTypeEnums b : BehaviorTypeEnums.values()) {
            if (b.code() == code) {
                return b;
            }
        }
        // 抛出异常或返回null，这里选择抛出异常更符合严谨性
        throw new IllegalArgumentException("无效的操作类型编码: " + code);
    }

    // Getter方法
    public int code() {
        return code;
    }

    public String desc() {
        return desc;
    }
}
