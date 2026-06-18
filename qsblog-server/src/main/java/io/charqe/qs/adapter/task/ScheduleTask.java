package io.charqe.qs.adapter.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
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


    @Resource
    private AccessLogService accessLogService;
    @Resource
    private QingSiCache qingSiCache;

    // @Scheduled(cron = "0 0 0 * * ?")
    public void cleanIpHistory() {
        CopyOnWriteArraySet<String> ipHistory =
                qingSiCache.get(CommonConst.IP_HISTORY) == null ? new CopyOnWriteArraySet<>()
                        : JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
                });
        if (ipHistory == null) {
            ipHistory = new CopyOnWriteArraySet<>();
            qingSiCache.put(CommonConst.IP_HISTORY, ipHistory);
        }
        ipHistory.clear();

        qingSiCache.del(CommonConst.IP_HISTORY_STATISTICS);
        Map<String, Object> ipStatisticsMap = new HashMap<>();

        accessLogService.addIPCache(ipStatisticsMap);
    }
}


