package io.charqe.qs.modules.file.enums;

import io.charqe.qs.common.enums.BaseEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * 微言/动态
     */
    MOMENT(2, "微言/动态"),

    /**
     * 评论
     */
    COMMENT(3, "评论"),

    /**
     * 相册
     */
    PHOTO(4, "相册"),

    /**
     * 视频
     */
    VIDEO(5, "视频"),

    /**
     * 音乐
     */
    MUSIC(6, "音乐"),

    /**
     * 分类/专栏
     */
    CATEGORY(7, "分类/专栏"),

    /**
     * 标签
     */
    TAG(8, "标签");

    /**
     * code -> enum 缓存，O(1) 查找
     */
    private static final Map<Integer, BizTypeEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.collectingAndThen(
                    Collectors.toMap(BizTypeEnum::getCode, Function.identity()),
                    Collections::unmodifiableMap
            ));

    /**
     * desc -> enum 缓存，O(1) 查找
     */
    private static final Map<String, BizTypeEnum> DESC_MAP = Arrays.stream(values())
            .collect(Collectors.collectingAndThen(
                    Collectors.toMap(BizTypeEnum::getDesc, Function.identity()),
                    Collections::unmodifiableMap
            ));

    private final Integer code;
    private final String desc;

    BizTypeEnum(Integer code, String desc) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.desc = Objects.requireNonNull(desc, "desc must not be null");
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /* ==================== code 相关 ==================== */

    /**
     * 根据编码获取枚举实例
     *
     * @param code 编码
     * @return 枚举实例，若不存在则返回 null
     */
    public static BizTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        return CODE_MAP.get(code);
    }

    /**
     * 根据编码获取枚举实例，返回 Optional
     *
     * @param code 编码
     * @return Optional 包装的枚举实例
     */
    public static Optional<BizTypeEnum> ofCode(Integer code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }

    /**
     * 根据编码获取枚举实例，不存在抛异常
     *
     * @param code 编码
     * @return 枚举实例
     * @throws IllegalArgumentException 编码非法时抛出
     */
    public static BizTypeEnum ofCodeOrThrow(Integer code) {
        return ofCode(code).orElseThrow(() ->
                new IllegalArgumentException("未知的资源类型编码: " + code));
    }

    /**
     * 根据编码获取描述
     *
     * @param code 编码
     * @return 描述，若不存在则返回 null
     */
    public static String getDescByCode(Integer code) {
        return ofCode(code).map(BizTypeEnum::getDesc).orElse(null);
    }

    /**
     * 根据编码获取描述，不存在返回默认值
     *
     * @param code         编码
     * @param defaultValue 默认值
     * @return 描述或默认值
     */
    public static String getDescByCode(Integer code, String defaultValue) {
        return ofCode(code).map(BizTypeEnum::getDesc).orElse(defaultValue);
    }

    /**
     * 验证编码是否有效
     *
     * @param code 编码
     * @return true 有效，false 无效
     */
    public static boolean isValid(Integer code) {
        return code != null && CODE_MAP.containsKey(code);
    }

    /* ==================== desc 相关 ==================== */

    /**
     * 根据描述获取枚举实例
     *
     * @param desc 描述
     * @return 枚举实例，若不存在则返回 null
     */
    public static BizTypeEnum getByDesc(String desc) {
        if (desc == null) {
            return null;
        }
        return DESC_MAP.get(desc);
    }

    /**
     * 根据描述获取枚举实例，返回 Optional
     *
     * @param desc 描述
     * @return Optional 包装的枚举实例
     */
    public static Optional<BizTypeEnum> ofDesc(String desc) {
        return Optional.ofNullable(desc).map(DESC_MAP::get);
    }

    /**
     * 根据描述获取枚举实例，不存在抛异常
     *
     * @param desc 描述
     * @return 枚举实例
     * @throws IllegalArgumentException 描述非法时抛出
     */
    public static BizTypeEnum ofDescOrThrow(String desc) {
        return ofDesc(desc).orElseThrow(() ->
                new IllegalArgumentException("未知的资源类型描述: " + desc));
    }

    /**
     * 根据描述获取编码
     *
     * @param desc 描述
     * @return 编码，若不存在则返回 null
     */
    public static Integer getCodeByDesc(String desc) {
        return ofDesc(desc).map(BizTypeEnum::getCode).orElse(null);
    }

    /**
     * 根据描述获取编码，不存在返回默认值
     *
     * @param desc         描述
     * @param defaultValue 默认值
     * @return 编码或默认值
     */
    public static Integer getCodeByDesc(String desc, Integer defaultValue) {
        return ofDesc(desc).map(BizTypeEnum::getCode).orElse(defaultValue);
    }

    /**
     * 验证描述是否有效
     *
     * @param desc 描述
     * @return true 有效，false 无效
     */
    public static boolean isValidDesc(String desc) {
        return desc != null && DESC_MAP.containsKey(desc);
    }
}