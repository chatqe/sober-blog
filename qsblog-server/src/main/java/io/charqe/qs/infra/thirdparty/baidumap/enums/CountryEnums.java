package io.charqe.qs.infra.thirdparty.baidumap.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 百度地图API国家前缀枚举
 *
 * @author Joy
 * @since 2026-01-24 15:48
 */
@Getter
public enum CountryEnums {

    // ===================== 亚洲地区（按编码排序） =====================
    CN("中国", "China"),
    JP("日本", "Japan"),
    KR("韩国", "South Korea"),
    SG("新加坡", "Singapore"),
    IN("印度", "India"),
    TH("泰国", "Thailand"),
    ID("印度尼西亚", "Indonesia"),
    MY("马来西亚", "Malaysia"),
    PH("菲律宾", "Philippines"),

    // ===================== 欧洲地区 =====================
    GB("英国", "United Kingdom"),
    DE("德国", "Germany"),
    FR("法国", "France"),
    IT("意大利", "Italy"),
    ES("西班牙", "Spain"),
    RU("俄罗斯", "Russia"),
    PT("葡萄牙", "Portugal"),
    NL("荷兰", "Netherlands"),

    // ===================== 美洲地区 =====================
    US("美国", "United States"),
    CA("加拿大", "Canada"),
    BR("巴西", "Brazil"),
    AR("阿根廷", "Argentina"),
    MX("墨西哥", "Mexico"),

    // ===================== 大洋洲地区 =====================
    AU("澳大利亚", "Australia"),
    NZ("新西兰", "New Zealand"),

    // ===================== 非洲地区 =====================
    ZA("南非", "South Africa"),
    NG("尼日利亚", "Nigeria"),
    EG("埃及", "Egypt"),

    // ===================== 兜底枚举（必须放在最后） =====================
    UNKNOWN("未知国家/地区", "Unknown Country/Region");

    /**
     * 中文名称（核心映射字段，用于业务展示）
     */
    private final String name;

    /**
     * 英文名称（用于国际化场景）
     */
    private final String englishName;

    /**
     * 编码→枚举映射缓存（静态初始化，避免每次查询遍历枚举）
     */
    private static final Map<String, CountryEnums> CODE_ENUM_MAP;

    // 静态代码块：初始化映射缓存（仅执行一次，提升查询性能）
    static {
        CODE_ENUM_MAP = new HashMap<>(CountryEnums.values().length);
        for (CountryEnums e : CountryEnums.values()) {
            CODE_ENUM_MAP.put(e.name(), e);
        }
    }

    CountryEnums(String chineseName, String englishName) {
        this.name = chineseName;
        this.englishName = englishName;
    }

    /**
     * 核心方法：根据国家编码获取枚举对象（含空值/异常兜底）
     * @param countryCode 国家编码（支持大小写，如CN/cn/Cn）
     * @return 对应的枚举对象，无匹配时返回UNKNOWN
     */
    public static CountryEnums getByCode(String countryCode) {
        // 1. 空值兜底：编码为空直接返回UNKNOWN
        if (StringUtils.isBlank(countryCode)) {
            return UNKNOWN;
        }

        // 2. 统一编码为大写（兼容不同大小写输入）
        String upperCode = countryCode.trim().toUpperCase();

        // 3. 从缓存映射中查询，无匹配返回UNKNOWN
        return Optional.ofNullable(CODE_ENUM_MAP.get(upperCode)).orElse(UNKNOWN);
    }


    /**
     * 快捷方法：根据编码获取中文名称
     * @param countryCode 国家编码
     * @return 中文名称，无匹配时返回"未知国家/地区"
     */
    public static String getNameByCode(String countryCode) {
        return getByCode(countryCode).getName();
    }

    /**
     * 快捷方法：根据编码获取英文名称
     * @param countryCode 国家编码
     * @return 英文名称，无匹配时返回"Unknown Country/Region"
     */
    public static String getEnglishNameByCode(String countryCode) {
        return getByCode(countryCode).getEnglishName();
    }

    /**
     * 校验方法：判断编码是否为中国（业务高频场景快捷判断）
     * @param countryCode 国家编码
     * @return true=中国，false=其他国家/未知
     */
    public static boolean isChina(String countryCode) {
        return CN.name().equals(StringUtils.trimToEmpty(countryCode).toUpperCase());
    }
}
