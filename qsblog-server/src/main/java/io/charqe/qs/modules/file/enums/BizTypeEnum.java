package io.charqe.qs.modules.file.enums;

import io.charqe.qs.common.enums.BaseEnum;

import java.util.Arrays;
import java.util.Objects;

/**
 * 资源类型枚举
 *
 * @author system
 * @since 2026-01-17
 */
public enum BizTypeEnum implements BaseEnum<Integer> {
    /**
     * 文章
     */
    ARTICLE(1, "文章"),

    /**
     * 评论
     */
    COMMENT(2, "评论"),

    /**
     * 视频
     */
    VIDEO(3, "视频"),

    /**
     * 音乐
     */
    MUSIC(4, "音乐");

    private final int code;
    private final String desc;

    BizTypeEnum(int code, String desc) {
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
    public static BizTypeEnum getByCode(Integer code) {
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