package io.charqe.qs.modules.comment.enums;

import io.charqe.qs.common.enums.BaseEnum;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论类型枚举
 *
 * @author system
 * @since 2026-01-17
 */
public enum CommentTypeEnum implements BaseEnum<String> {
    /**
     * 文章评论
     */
    ARTICLE("article", "文章评论"),
    
    /**
     * 树洞留言
     */
    MESSAGE("message", "树洞留言"),
    
    /**
     * 表白墙留言
     */
    LOVE("love", "表白墙留言");

    private final String code;
    private final String desc;

    CommentTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public String getCode() {
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
    public static CommentTypeEnum getByCode(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(enumItem -> enumItem.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据描述获取枚举实例
     *
     * @param desc 描述
     * @return 枚举实例，若不存在则返回null
     */
    public static CommentTypeEnum getByDesc(String desc) {
        if (!StringUtils.hasText(desc)) {
            return null;
        }
        return Arrays.stream(values())
                .filter(enumItem -> enumItem.desc.equalsIgnoreCase(desc))
                .findFirst()
                .orElse(null);
    }

    /**
     * 验证编码是否有效
     *
     * @param code 编码
     * @return true 有效，false 无效
     */
    public static boolean isValid(String code) {
        return getByCode(code) != null;
    }

    /**
     * 获取枚举映射（编码到描述）
     *
     * @return 枚举映射
     */
    public static Map<String, String> getEnumMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(CommentTypeEnum::getCode, CommentTypeEnum::getDesc));
    }
}


