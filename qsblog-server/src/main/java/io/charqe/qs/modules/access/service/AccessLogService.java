package io.charqe.qs.modules.access.service;

import io.charqe.qs.common.model.mp.CoreService;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;

import java.util.List;
import java.util.Map;

/**
 * 访问日志服务
 *
 * @author Joy
 * @since 2026-01-23 20:07
 */

public interface AccessLogService extends CoreService<AccessLog> {

    /**
     * 获取访问IP最多的10个省 各省份独立 IP 数
     *
     * @return
     */
    List<Map<String, Object>> getTop10ProvincesByIP();

    /**
     * 访问次数最多的10个IP
     */
    List<Map<String, Object>> getTop10IPs();

    /**
     * 获取最近24小时的数据
     */
    List<Map<String, Object>> getLast24HoursData();

    /**
     * 获取总访问量
     */
    Long allAccessCount();

    /**
     * 获取站点累计访问量（缓存优先，缓存缺失时回源统计并重建缓存）
     */
    long getHistoryAllCount();

    /**
     * 添加IP统计相关数据缓存
     */
    void addIPCache(Map<String, Object> historyMap);
}
