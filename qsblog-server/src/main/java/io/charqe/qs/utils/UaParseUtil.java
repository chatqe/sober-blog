package io.charqe.qs.utils;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * User-Agent 解析工具（基于 yauaa）。
 * <p>
 * {@link UserAgentAnalyzer} 构建较重（需加载大量解析规则、占用内存），
 * 因此在容器启动时构建一次并作为单例持有；其 parse 方法线程安全且内置缓存，
 * 可在多线程（含异步日志线程）中并发调用，<b>切勿每次请求 new</b>。
 *
 * @author Joy
 * @since 2026-09-06
 */
@Component
public class UaParseUtil {

    /** browser 列长度 varchar(64) */
    private static final int BROWSER_MAX_LEN = 64;
    /** os 列长度 varchar(64) */
    private static final int OS_MAX_LEN = 64;
    /** device_type 列长度 varchar(32) */
    private static final int DEVICE_MAX_LEN = 32;

    private final UserAgentAnalyzer analyzer;

    public UaParseUtil() {
        this.analyzer = UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                // 解析结果缓存，避免相同 UA 重复匹配
                .withCache(10_000)
                // 仅加载需要的字段，显著降低初始化耗时与内存占用
                .withField("DeviceClass")
                .withField("AgentName")
                .withField("OperatingSystemName")
                .build();
    }

    /**
     * 解析 User-Agent 字符串。
     *
     * @param userAgent 请求头 User-Agent 原文
     * @return 解析结果；入参为空时各字段均为 null
     */
    public UaInfo parse(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return new UaInfo(null, null, null);
        }
        UserAgent agent = analyzer.parse(userAgent);
        String browser = normalize(agent.getValue("AgentName"), BROWSER_MAX_LEN);
        String os = normalize(agent.getValue("OperatingSystemName"), OS_MAX_LEN);
        String deviceType = mapDeviceType(agent.getValue("DeviceClass"));
        return new UaInfo(browser, os, deviceType);
    }

    /**
     * yauaa 未识别字段返回 "Unknown"，统一归一为 null 并按列长截断
     */
    private String normalize(String value, int maxLen) {
        if (!StringUtils.hasText(value) || "Unknown".equalsIgnoreCase(value.trim())) {
            return null;
        }
        String v = value.trim();
        return v.length() > maxLen ? v.substring(0, maxLen) : v;
    }

    /**
     * 将 yauaa 的 DeviceClass 映射为业务设备类型（对齐 access_log.device_type 注释：PC/Mobile/Pad）。
     * 未覆盖的设备类型（TV/Watch 等）保留原值并截断；无法识别返回 null。
     */
    private String mapDeviceType(String deviceClass) {
        if (!StringUtils.hasText(deviceClass) || "Unknown".equalsIgnoreCase(deviceClass.trim())) {
            return null;
        }
        String v = deviceClass.trim();
        switch (v) {
            case "Desktop":
                return "PC";
            case "Phone":
                return "Mobile";
            case "Tablet":
            case "eReader":
                return "Pad";
            default:
                return v.length() > DEVICE_MAX_LEN ? v.substring(0, DEVICE_MAX_LEN) : v;
        }
    }

    /**
     * UA 解析结果
     *
     * @param browser    浏览器名称（如 Chrome、Firefox、WeChat）
     * @param os         操作系统（如 Windows、macOS、Android、iOS）
     * @param deviceType 设备类型（PC/Mobile/Pad 等）
     */
    public record UaInfo(String browser, String os, String deviceType) {
    }
}
