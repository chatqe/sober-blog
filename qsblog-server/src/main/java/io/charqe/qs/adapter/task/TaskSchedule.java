package io.charqe.qs.adapter.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.UserTypeEnum;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.modules.family.service.FamilyService;
import io.charqe.qs.modules.historyInfo.domain.dao.HistoryInfoMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.modules.webInfo.domain.dao.WebInfoMapper;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * 定时任务类
 *
 * @author EDY
 * @since 2024-05-21 16:44
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "scheduling", name = "enabled", havingValue = "true") // 只有当yml中配置属性为true才生效
public class TaskSchedule {

    @Value("${store.type}")
    private String defaultType;

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private FamilyService familyService;

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    @Resource
    private AccessLogService accessLogService;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 定时创建 ipHistory 相关数据
     *
     * @throws Exception
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void updateBaseData() throws Exception {
        log.info("--------------->开始更新基础数据");
        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.get(0).setDefaultStoreType(defaultType);

            qingSiCache.put(CommonConst.WEB_INFO, list.get(0));
        }

        User admin = userService.lambdaQuery().eq(User::getUserType, UserTypeEnum.ADMIN.getCode()).one();
        // PoetryCache.put(CommonConst.ADMIN, admin);
        qingSiCache.put(CommonConst.ADMIN, admin);

        Family family = familyService.lambdaQuery().eq(Family::getUserId, admin.getId()).one();
        //
        qingSiCache.put(CommonConst.ADMIN_FAMILY, family);

        // 查询 HistoryInfo 表中所有在今天零点之后创建的记录。
        // 只返回 ip 和 userId 字段。
        // List<HistoryInfo> infoList = new LambdaQueryChainWrapper<>(historyInfoMapper)
        //         .select(HistoryInfo::getIp, HistoryInfo::getUserId)
        //         .ge(HistoryInfo::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
        //         .list();

        List<AccessLog> infoList = accessLogService.list(
                Lb.<AccessLog>q()
                        .ge(AccessLog::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
                        .select(AccessLog::getIp, AccessLog::getUserId));

        CopyOnWriteArraySet<String> strings = new CopyOnWriteArraySet<>(
                infoList.stream().map(info -> info.getIp() + (info.getUserId() != null ? "_" + info.getUserId().toString() : "")).collect(Collectors.toList()));

        // PoetryCache.put(CommonConst.IP_HISTORY, strings);
        qingSiCache.put(CommonConst.IP_HISTORY, strings);

        Map<String, Object> history = new HashMap<>();
        history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount() + 1168);
        history.put(CommonConst.IP_HISTORY_PROVINCE, accessLogService.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, accessLogService.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, accessLogService.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, accessLogService.getHistoryCount() + 1168);

        log.info("当天更新缓存历史信息:{}", history);
        qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, history);

        // 初始化和启动WebSocket
        // TioUtil.buildTio();
        // TioWebsocketStarter websocketStarter = TioUtil.getTio();
        // if (websocketStarter != null) {
        //     websocketStarter.start();
        // }
    }


    @Scheduled(cron = "0 1 0 * * ?")
    public void cleanIpHistory() {
        // CopyOnWriteArraySet<String> ipHistory = (CopyOnWriteArraySet<String>) PoetryCache.get(CommonConst.IP_HISTORY);
        CopyOnWriteArraySet<String> ipHistory = JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
        });
        if (ipHistory == null) {
            ipHistory = new CopyOnWriteArraySet<>();
            // PoetryCache.put(CommonConst.IP_HISTORY, ipHistory);
            qingSiCache.put(CommonConst.IP_HISTORY, ipHistory);
        }
        ipHistory.clear();

        // PoetryCache.remove(CommonConst.IP_HISTORY_STATISTICS);
        // qingSiCache.delete(CommonConst.IP_HISTORY_STATISTICS);
        // Map<String, Object> history = new HashMap<>();
        // history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        // history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        // history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        // history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount());
        // // PoetryCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
        // qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
    }

}


