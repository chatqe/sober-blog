package io.charqe.qs.adapter.task;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.user.enums.UserTypeEnum;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.modules.family.service.FamilyService;
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

        User admin = userService.lambdaQuery().eq(User::getUserType, UserTypeEnum.ADMIN.code()).one();
        qingSiCache.put(CommonConst.ADMIN, admin);

        Family family = familyService.lambdaQuery().eq(Family::getUserId, admin.getId()).one();
        //
        qingSiCache.put(CommonConst.ADMIN_FAMILY, family);

        List<AccessLog> infoList = accessLogService.list(
                Lb.<AccessLog>q()
                        .ge(AccessLog::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
                        .select(AccessLog::getIp, AccessLog::getUserId));

        CopyOnWriteArraySet<String> strings = new CopyOnWriteArraySet<>(
                infoList.stream().map(info -> info.getIp() + (info.getUserId() != null ? "_" + info.getUserId().toString() : "")).collect(Collectors.toList()));

        qingSiCache.put(CommonConst.IP_HISTORY, strings);

        Map<String, Object> ipStatisticsMap = new HashMap<>();
        log.info("当天更新缓存历史信息:{}", ipStatisticsMap);
        accessLogService.addIPCache( ipStatisticsMap);


        // 初始化和启动WebSocket
        // TioUtil.buildTio();
        // TioWebsocketStarter websocketStarter = TioUtil.getTio();
        // if (websocketStarter != null) {
        //     websocketStarter.start();
        // }
    }


    @Scheduled(cron = "0 1 0 * * ?")
    public void cleanIpHistory() {

        CopyOnWriteArraySet<String> ipHistory = JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY), new TypeReference<CopyOnWriteArraySet<String>>() {
        });
        if (ipHistory == null) {
            ipHistory = new CopyOnWriteArraySet<>();

            qingSiCache.put(CommonConst.IP_HISTORY, ipHistory);
        }

        ipHistory.clear();
    }

}


