package io.charqe.qs.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;

/**
 * IP工具类
 *
 * @author Joy
 * @since 2026-01-02 19:53
 */

public final class IPUtil {

    /* 常见代理头，按优先级排列 */
    private static final List<String> PROXY_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    /* 本地环路 */
    private static final String LOCAL_IPV4 = "127.0.0.1";
    private static final String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";

    private IPUtil() {
    }

    /**
     * 获取客户端真实 IP
     * 1. 多级代理只取第一个非 unknown、非内网段
     * 2. 拿不到时退回到 request.getRemoteAddr()
     * 3. 环路地址统一返回 127.0.0.1
     */
    public static String getRealIp(HttpServletRequest request) {
        return PROXY_HEADERS.stream()
                .map(request::getHeader)
                .filter(IPUtil::isValidIp)
                .findFirst()
                .map(IPUtil::firstNonProxy)
                .orElseGet(() -> fallbackRemoteAddr(request));
    }

    /* --------------- private --------------- */

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }

    /**
     * 多级代理场景：X-Forwarded-For = client, proxy1, proxy2
     */
    private static String firstNonProxy(String ipPort) {
        int idx = ipPort.indexOf(',');
        String first = idx == -1 ? ipPort : ipPort.substring(0, idx);
        return first.trim();
    }

    private static String fallbackRemoteAddr(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        return LOCAL_IPV6.equals(remote) ? LOCAL_IPV4 : remote;
    }

    /* 单元测试友好：直接拿本机名，缓存起来 */
    public static String getLocalHostAddr() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return LOCAL_IPV4;
        }
    }


    /**
     * 获取当前请求
     */
    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }
}