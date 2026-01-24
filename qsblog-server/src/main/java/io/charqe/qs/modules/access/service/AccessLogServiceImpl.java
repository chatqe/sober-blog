package io.charqe.qs.modules.access.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.charqe.qs.common.model.mp.CoreServiceImpl;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.access.domain.dao.AccessLogMapper;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import org.springframework.stereotype.Service;

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

    @Override
    public List<Map<String, Object>> getTop10ProvincesByIP() {
        QueryWrapper<AccessLog> wrapper = new QueryWrapper<>();
        wrapper.select("country", "province", "count(distinct ip) as num")
                .groupBy("country", "province")
                .orderByDesc("num")
                .last("limit 10");

        return baseMapper.selectMaps(wrapper);

    }

    @Override
    public List<Map<String, Object>> getTop10IPs() {
        return null;
    }

    @Override
    public List<Map<String, Object>> getLast24HoursData() {
        return null;
    }

    @Override
    public Long allAccessCount() {
        return null;
    }
}
