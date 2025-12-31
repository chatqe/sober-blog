package io.charqe.qs.adapter.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.historyInfo.domain.dao.HistoryInfoMapper;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@EnableScheduling
@Slf4j
public class ScheduleTask {

    @Autowired
    private HistoryInfoMapper historyInfoMapper;
    @Resource
    private QingSiCache qingSiCache;

    // @Scheduled(cron = "0 0 0 * * ?")
    public void cleanIpHistory() {
        // CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) PoetryCache.get(CommonConst.IP_HISTORY);
        CopyOnWriteArraySet<String> ipHistory =
                qingSiCache.get(CommonConst.IP_HISTORY) == null ? new CopyOnWriteArraySet<>()
                        : JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
                });
        if (ipHistory == null) {
            ipHistory = new CopyOnWriteArraySet<>();
            // PoetryCache.put(CommonConst.IP_HISTORY, ipHistory);
            qingSiCache.put(CommonConst.IP_HISTORY, ipHistory);
        }
        ipHistory.clear();

        // PoetryCache.remove(CommonConst.IP_HISTORY_STATISTICS);
        qingSiCache.del(CommonConst.IP_HISTORY_STATISTICS);
        Map<String, Object> history = new HashMap<>();
        history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount());
        // PoetryCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
        qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
    }
}


