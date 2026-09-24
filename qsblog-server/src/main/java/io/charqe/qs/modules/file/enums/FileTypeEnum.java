package io.charqe.qs.modules.file.enums;

import io.charqe.qs.common.enums.BaseEnum;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件类型枚举
 *
 * @author Joy
 * @since 2026-09-24 11:07
 */
@Schema(description = "文件类型枚举")
public enum FileTypeEnum implements BaseEnum<Integer> {

    IMAGE(1, "图片"),
    VIDEO(2, "视频"),
    AUDIO(3, "音频"),
    DOCUMENT(4, "文档"),
    OTHER(5, "其他");

    /**
     * code -> enum 缓存，避免每次 values() 遍历
     */
    private static final Map<Integer, FileTypeEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.collectingAndThen(
                    Collectors.toMap(FileTypeEnum::getCode, Function.identity()),
                    Collections::unmodifiableMap
            ));

    /**
     * desc -> enum 缓存
     */
    private static final Map<String, FileTypeEnum> DESC_MAP = Arrays.stream(values())
            .collect(Collectors.collectingAndThen(
                    Collectors.toMap(FileTypeEnum::getDesc, Function.identity()),
                    Collections::unmodifiableMap
            ));

    private final Integer code;
    private final String desc;

    FileTypeEnum(Integer code, String desc) {
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
     * 根据 code 获取枚举，未知返回 Optional.empty()
     */
    public static Optional<FileTypeEnum> ofCode(Integer code) {
        return Optional.ofNullable(CODE_MAP.get(code));
    }

    /**
     * 根据 code 获取枚举，未知抛异常
     */
    public static FileTypeEnum ofCodeOrThrow(Integer code) {
        return ofCode(code).orElseThrow(() ->
                new IllegalArgumentException("未知的文件类型编码: " + code));
    }

    /**
     * 根据 code 获取描述，未知返回 null
     */
    public static String getDescByCode(Integer code) {
        return ofCode(code).map(FileTypeEnum::getDesc).orElse(null);
    }

    /**
     * 根据 code 获取描述，未知返回默认值
     */
    public static String getDescByCode(Integer code, String defaultValue) {
        return ofCode(code).map(FileTypeEnum::getDesc).orElse(defaultValue);
    }

    /**
     * 判断 code 是否合法
     */
    public static boolean isValidCode(Integer code) {
        return CODE_MAP.containsKey(code);
    }

    /* ==================== desc 相关 ==================== */

    /**
     * 根据 desc 获取枚举，未知返回 Optional.empty()
     */
    public static Optional<FileTypeEnum> ofDesc(String desc) {
        return Optional.ofNullable(desc).map(DESC_MAP::get);
    }

    /**
     * 根据 desc 获取枚举，未知抛异常
     */
    public static FileTypeEnum ofDescOrThrow(String desc) {
        return ofDesc(desc).orElseThrow(() ->
                new IllegalArgumentException("未知的文件类型描述: " + desc));
    }

    /**
     * 根据 desc 获取 code，未知返回 null
     */
    public static Integer getCodeByDesc(String desc) {
        return ofDesc(desc).map(FileTypeEnum::getCode).orElse(null);
    }

    /**
     * 根据 desc 获取 code，未知返回默认值
     */
    public static Integer getCodeByDesc(String desc, Integer defaultValue) {
        return ofDesc(desc).map(FileTypeEnum::getCode).orElse(defaultValue);
    }

    /**
     * 判断 desc 是否合法
     */
    public static boolean isValidDesc(String desc) {
        return desc != null && DESC_MAP.containsKey(desc);
    }
}
