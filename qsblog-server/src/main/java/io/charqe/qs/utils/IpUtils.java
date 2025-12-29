package io.charqe.qs.utils;

import javax.servlet.http.HttpServletRequest;

/**
 * ip工具类
 *
 * @author sjq
 * @since 2025-11-30 15:36
 */
public final class IpUtils {

    /**
     * 获取客户端真实 IP
     * 1. 优先取 X-Forwarded-For 第一段
     * 2. 其次取 X-Real-IP
     * 3. 最后兜底 request.getRemoteAddr()
     * <p>
     * 注意：
     * - XFF 可能用逗号分隔多级代理，第一段即源头
     * - 存在伪造风险，只能用于“幂等维度”或“限流”，不能用于安全鉴权
     */
    public static String getRealIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时，XFF=client,proxy1,proxy2
            int idx = ip.indexOf(',');
            return idx != -1 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        // 没有代理时直接取 TCP 对端地址
        return request.getRemoteAddr();
    }

    private IpUtils() {
    }
}


