package io.charqe.qs.modules.access.controller.admin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.R;
import io.charqe.qs.common.model.mp.Lb;
import io.charqe.qs.modules.access.domain.pojo.AccessLog;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理后台-历史数据
 *
 * @author sjq
 * @since 2025-12-31 20:09
 */
@Slf4j
@RestController
@RequestMapping("admin/accessLog")
@Tag(name = "adminAccessLog", description = "管理后台-访问数据")
public class AdminAccessLogController {

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private CommonQuery commonQuery;

    @Resource
    private AccessLogService accessLogService;

    /**
     * 获取网站统计信息
     */
    @LoginCheck(0)
    @GetMapping("/getData")
    public R<Map<String, Object>> getAccessLogData() {

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> history = JSON.parseObject(
                qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS), new TypeReference<Map<String, Object>>() {
                });

        // 获取今天之前的 ip,userId,国家,省,市
        List<AccessLog> infoList = accessLogService.list(Lb.<AccessLog>q()
                .select(AccessLog::getIp, AccessLog::getUserId, AccessLog::getCountry, AccessLog::getProvince, AccessLog::getCity)
                .ge(AccessLog::getCreateTime, LocalDateTime.now().with(LocalTime.MIN)));

        result.put(CommonConst.IP_HISTORY_PROVINCE, history.get(CommonConst.IP_HISTORY_PROVINCE));
        result.put(CommonConst.IP_HISTORY_IP, history.get(CommonConst.IP_HISTORY_IP));
        result.put(CommonConst.IP_HISTORY_COUNT, history.get(CommonConst.IP_HISTORY_COUNT));
        List<Map<String, Object>> ipHistoryCount = (List<Map<String, Object>>) history.get(CommonConst.IP_HISTORY_HOUR);

        // 昨日访问数
        result.put("ip_count_yest", ipHistoryCount.stream().map(m -> m.get("ip")).distinct().count());
        result.put("username_yest", ipHistoryCount.stream().map(m -> {
            Object userId = m.get("user_id");
            if (userId != null) {
                User user = commonQuery.getUser(Long.valueOf(userId.toString()));
                if (user != null) {
                    Map<String, String> userInfo = new HashMap<>();
                    userInfo.put("avatar", user.getAvatar());
                    userInfo.put("username", user.getUsername());
                    return userInfo;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()));

        // 今日访问数
        result.put("ip_count_today", infoList.stream().map(AccessLog::getIp).distinct().count());

        // 今日访问用户
        result.put("username_today", infoList.stream().map(m -> {
            Long userId = m.getUserId();
            if (userId != null) {
                User user = commonQuery.getUser(userId);
                if (user != null) {
                    Map<String, String> userInfo = new HashMap<>();
                    userInfo.put("avatar", user.getAvatar());
                    userInfo.put("username", user.getUsername());
                    return userInfo;
                }
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()));

        List<Map<String, Object>> list = infoList.stream()
                .map(AccessLog::getProvince).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("province", entry.getKey());
                    map.put("num", entry.getValue());
                    return map;
                })
                .sorted((o1, o2) -> Long.valueOf(o2.get("num").toString()).compareTo(Long.valueOf(o1.get("num").toString())))
                .collect(Collectors.toList());

        // 今日访问省份统计
        result.put(CommonConst.PROVINCE_TODAY, list);

        // 获取今日IP访问详情
        result.put(CommonConst.IP_DETAIL_TODAY, null);

        return R.success(result);
    }
}
