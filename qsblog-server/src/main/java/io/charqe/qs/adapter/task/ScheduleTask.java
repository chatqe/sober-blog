package io.charqe.qs.adapter.task;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
        // 缓存中以 JSON 数组形式存储（["ip_userId", ...]），需用 parseArray 解析；
        // fastjson2 的 parseObject 期望对象 '{'，遇到数组首字符 '[' 会抛 JSONException
        String cachedIpHistory = qingSiCache.get(CommonConst.IP_HISTORY);
        CopyOnWriteArraySet<String> ipHistory = StringUtils.hasText(cachedIpHistory)
                ? new CopyOnWriteArraySet<>(JSON.parseArray(cachedIpHistory, String.class))
                : new CopyOnWriteArraySet<>();
        if (ipHistory.isEmpty()) {
            qingSiCache.put(CommonConst.IP_HISTORY, ipHistory);
        }
        ipHistory.clear();

        qingSiCache.del(CommonConst.IP_HISTORY_STATISTICS);
        Map<String, Object> ipStatisticsMap = new HashMap<>();

        accessLogService.addIPCache(ipStatisticsMap);
    }
}


