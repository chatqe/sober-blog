package io.charqe.qs.common.config;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.user.enums.UserTypeEnum;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.family.domain.pojo.Family;
import io.charqe.qs.modules.family.service.FamilyService;
import io.charqe.qs.modules.im.websocket.TioUtil;
import io.charqe.qs.modules.im.websocket.TioWebsocketStarter;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.service.UserService;
import io.charqe.qs.modules.webInfo.domain.dao.WebInfoMapper;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
 * 项目启动初始化
 */
@Component
public class YouthApplicationRunner implements ApplicationRunner {

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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.get(0).setDefaultStoreType(defaultType);
            qingSiCache.put(CommonConst.WEB_INFO, list.get(0));
        }

        User admin = userService.lambdaQuery().eq(User::getUserType, UserTypeEnum.ADMIN.code()).last("limit 1").one();
        qingSiCache.put(CommonConst.ADMIN, admin);

        Family family = familyService.lambdaQuery().eq(Family::getUserId, admin.getId()).one();
        qingSiCache.put(CommonConst.ADMIN_FAMILY, family);

        List<AccessLog> infoList = accessLogService.list(Lb.<AccessLog>q()
                .select(AccessLog::getIp, AccessLog::getUserId)
                .eq(AccessLog::getCreateTime, LocalDateTime.now().with(LocalTime.MIN)));

        qingSiCache.put(CommonConst.IP_HISTORY, new CopyOnWriteArraySet<>(infoList.stream().map(info -> info.getIp() + (info.getUserId() != null ? "_" + info.getUserId().toString() : "")).collect(Collectors.toList())));

        Map<String, Object> ipStatisticsMap = new HashMap<>();

        accessLogService.addIPCache(ipStatisticsMap);
        // 初始化和启动WebSocket
        TioUtil.buildTio();
        TioWebsocketStarter websocketStarter = TioUtil.getTio();
        if (websocketStarter != null) {
            websocketStarter.start();
        }
    }
}


