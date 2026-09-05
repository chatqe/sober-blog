package io.charqe.qs.utils;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
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

    /** 本机局域网 IP 缓存（网卡 IP 运行期基本不变，避免每个请求重复探测/遍历网卡） */
    private static volatile String localLanIpCache;

    private IPUtil() {
    }

    /**
     * 获取客户端真实 IP
     * 1. 多级代理只取第一个非 unknown、非内网段
     * 2. 拿不到时退回到 request.getRemoteAddr()
     * 3. 若最终为本机回环地址（127.0.0.1 / 0:0:0:0:0:0:0:1 / ::1），
     *    说明请求来自本机，改用网卡获取本机配置的局域网 IP
     */
    public static String getRealIp(HttpServletRequest request) {
        String ip = PROXY_HEADERS.stream()
                .map(request::getHeader)
                .filter(IPUtil::isValidIp)
                .findFirst()
                .map(IPUtil::firstNonProxy)
                .orElseGet(() -> fallbackRemoteAddr(request));
        // 本机请求：回环地址替换为网卡真实 IP，便于访问日志/统计记录实际地址
        return isLoopbackIp(ip) ? resolveLocalLanIp() : ip;
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

    /**
     * 判断是否为本机回环地址
     * 覆盖 IPv4（127.x.x.x）与 IPv6（0:0:0:0:0:0:0:1 / ::1）
     */
    private static boolean isLoopbackIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return ip.startsWith("127.")
                || LOCAL_IPV6.equals(ip)
                || "::1".equals(ip);
    }

    /**
     * 根据网卡获取本机配置的局域网 IP。
     * <p>
     * 选择策略（确定性优先级，避免多网卡/虚拟网卡污染）：
     * <ol>
     *   <li>默认出口路由探测：用 UDP Socket connect 到外部地址（不实际发包），
     *       取系统默认出口网卡绑定的本机 IPv4，最贴近真实访问地址；</li>
     *   <li>遍历网卡：枚举已启用、非回环、非虚拟的网卡，优先返回站点本地（私有网段）IPv4，
     *       剔除 169.254.x.x 链路本地地址；</li>
     *   <li>兜底：{@link InetAddress#getLocalHost()}，再失败返回 127.0.0.1。</li>
     * </ol>
     * 注意：不依赖 {@code InetAddress.getLocalHost()} 作为首选，因其在 Windows
     * 多网卡（VMware/Hyper-V/网络共享 192.168.137.x）环境下可能返回虚拟网卡地址。
     *
     * @return 本机局域网 IPv4；极端情况下回退到 127.0.0.1
     */
    private static String resolveLocalLanIp() {
        // 结果缓存：本机网卡 IP 运行期基本不变，双重检查锁避免每请求重复探测
        String cached = localLanIpCache;
        if (cached != null) {
            return cached;
        }
        synchronized (IPUtil.class) {
            if (localLanIpCache == null) {
                localLanIpCache = doResolveLocalLanIp();
            }
            return localLanIpCache;
        }
    }

    /**
     * 实际执行本机局域网 IP 解析（仅在缓存未命中时调用一次）
     */
    private static String doResolveLocalLanIp() {
        // 1. 默认出口路由探测（connect 不会真正发送数据包）
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            InetAddress local = socket.getLocalAddress();
            if (local instanceof Inet4Address && !local.isLoopbackAddress()) {
                return local.getHostAddress();
            }
        } catch (Exception ignored) {
            // 无外网或受限环境，降级为网卡遍历
        }

        // 2. 遍历网卡
        try {
            String firstCandidate = null;
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!(addr instanceof Inet4Address) || addr.isLoopbackAddress()) {
                        continue;
                    }
                    String host = addr.getHostAddress();
                    // 剔除 DHCP 失败时的链路本地地址 169.254.x.x
                    if (host == null || host.startsWith("169.254.")) {
                        continue;
                    }
                    // 优先返回私有网段（10.x / 172.16-31.x / 192.168.x）
                    if (addr.isSiteLocalAddress()) {
                        return host;
                    }
                    if (firstCandidate == null) {
                        firstCandidate = host;
                    }
                }
            }
            if (firstCandidate != null) {
                return firstCandidate;
            }
            // 3. 兜底
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException ex) {
                return LOCAL_IPV4;
            }
        }
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