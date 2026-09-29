package io.charqe.qs.modules.access.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.modules.access.domain.dao.AccessLogMapper;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 访问日志实现类
 *
 * @author Joy
 * @since 2026-01-23 20:52
 */
@Service
public class AccessLogServiceImpl extends CoreServiceImpl<AccessLogMapper, AccessLog> implements AccessLogService {

    @Resource
    private QingSiCache qingSiCache;

    @Override
    public List<Map<String, Object>> getTop10ProvincesByIP() {
        QueryWrapper<AccessLog> wrapper = Wrappers.<AccessLog>query();
        wrapper.select("country", "province", "count(distinct ip) as num")
                .groupBy("country", "province")
                .orderByDesc("num")
                .last("limit 10");

        return baseMapper.selectMaps(wrapper);

    }

    @Override
    public List<Map<String, Object>> getTop10IPs() {
        QueryWrapper<AccessLog> wrapper = Wrappers.<AccessLog>query();
        wrapper.select("ip", "count(*) as num") // 聚合查询
                .groupBy("ip") // 按IP分组
                .orderByDesc("num") // 按数量降序
                .last("limit 10"); // 限制前10条

        return baseMapper.selectMaps(wrapper);
    }

    @Override
    public List<Map<String, Object>> getLast24HoursData() {
        QueryWrapper<AccessLog> wrapper = Wrappers.<AccessLog>query();

        // Java 计算24小时前的时间
        LocalDateTime before24Hour = LocalDateTime.now().minus(24, ChronoUnit.HOURS);

        wrapper.select("ip", "user_id", "country", "province")
                .ge("create_time", before24Hour);

        return baseMapper.selectMaps(wrapper);
    }

    @Override
    public Long allAccessCount() {
        return count();
    }

    @Override
    public void addIPCache(Map<String, Object> ipStatisticsMap) {
        ipStatisticsMap.put(CommonConst.IP_HISTORY_PROVINCE, getTop10ProvincesByIP());
        ipStatisticsMap.put(CommonConst.IP_HISTORY_IP, getTop10IPs());
        ipStatisticsMap.put(CommonConst.IP_HISTORY_HOUR, getLast24HoursData());
        ipStatisticsMap.put(CommonConst.IP_HISTORY_COUNT, allAccessCount() + 1168);
        // 缓存
        qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, JSON.toJSONString(ipStatisticsMap));
    }
}
