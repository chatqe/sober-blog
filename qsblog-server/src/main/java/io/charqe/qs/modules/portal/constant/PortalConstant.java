package io.charqe.qs.modules.portal.constant;

/**
 * 门户聚合模块常量
 *
 * @author Joy
 * @since 2026-10-23
 */
public final class PortalConstant {

    /**
     * 站点概览缓存 key
     */
    public static final String OVERVIEW_CACHE = "portal:overview";

    /**
     * 站点概览缓存时长（秒）
     * 浏览量/点赞数变化频繁，采用短 TTL 容忍分钟级延迟
     */
    public static final long OVERVIEW_CACHE_SECONDS = 60L;

    /**
     * 站长信息缓存 key
     */
    public static final String OWNER_CACHE = "portal:owner";

    /**
     * 站长信息缓存时长（秒）
     */
    public static final long OWNER_CACHE_SECONDS = 300L;

    private PortalConstant() {
    }
}
