package com.joy.qs.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.joy.qs.aop.LoginCheck;
import com.joy.qs.common.R;
import com.joy.qs.dao.HistoryInfoMapper;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.model.historyinfo.pojo.HistoryInfo;
import com.joy.qs.model.user.pojo.User;
import com.joy.qs.model.webinfo.pojo.WebInfo;
import com.joy.qs.service.WebInfoService;
import com.joy.qs.utils.CommonQuery;
import com.joy.qs.utils.cache.PoetryCache;
import com.joy.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 网站信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Slf4j
@RestController
@RequestMapping("/webInfo")
public class WebInfoController {

    @Value("${store.type}")
    private String defaultType;

    @Autowired
    private WebInfoService webInfoService;

    @Autowired
    private HistoryInfoMapper historyInfoMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;


    /**
     * 更新网站信息
     */
    @LoginCheck(0)
    @PostMapping("/updateWebInfo")
    public R<WebInfo> updateWebInfo(@RequestBody WebInfo webInfo) {
        webInfoService.updateById(webInfo);

        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoService.getBaseMapper());
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.get(0).setDefaultStoreType(defaultType);
            PoetryCache.put(CommonConst.WEB_INFO, list.get(0));
            qingSiCache.put(CommonConst.WEB_INFO, list.get(0));
        }
        return R.success();
    }


    /**
     * 获取网站信息
     */
    @GetMapping("/getWebInfo")
    public R<WebInfo> getWebInfo() {
        // WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        // WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        //
        // if (webInfo != null) {
        //     WebInfo result = new WebInfo();
        //     BeanUtils.copyProperties(webInfo, result);
        //     result.setRandomAvatar(null);
        //     result.setRandomName(null);
        //     result.setWaifuJson(null);
        //
        //
        //     Map<String, Object> historyMap = JSON.parseObject(
        //             qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS), new TypeReference<Map<String, Object>>() {
        //             });
        //     // 总访问数
        //     result.setHistoryAllCount( historyMap.getOrDefault(CommonConst.IP_HISTORY_COUNT, 1168).toString());
        //
        //     // 24小时内访问数
        //    Object historyDayData = historyMap.get(CommonConst.IP_HISTORY_HOUR);
        //     String dayHistoryStr =Objects.isNull(historyDayData)?"168":String.valueOf(JSON.parseObject(historyDayData.toString(), new TypeReference<List<HistoryInfo>>() {}).size());
        //
        //     result.setHistoryDayCount(dayHistoryStr);
        //     log.info("result:{}", result);
        //     return R.success(result);
        // }
        // // 为空 查数据库
        // webInfo = webInfoService.list().get(0);
        //
        // Map<String, Object> history = new HashMap<>();
        // history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        // history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        // history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        // history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount()+1168);
        // // PoetryCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
        // qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
        //
        // qingSiCache.put(CommonConst.WEB_INFO, webInfo);
        //
        // Map<String, Object> historyMap = JSON.parseObject(
        //         qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS), new TypeReference<Map<String, Object>>() {
        //         });
        // // 总访问数
        // webInfo.setHistoryAllCount( historyMap.getOrDefault(CommonConst.IP_HISTORY_COUNT, 1168).toString());
        //
        // // 24小时内访问数
        // Object historyDayData = historyMap.get(CommonConst.IP_HISTORY_HOUR);
        // String dayHistoryStr =Objects.isNull(historyDayData)?"168":String.valueOf(JSON.parseObject(historyDayData.toString(), new TypeReference<List<HistoryInfo>>() {}).size());
        //
        // webInfo.setHistoryDayCount(dayHistoryStr);
        // return R.success(webInfo);

        // 从缓存获取 WebInfo 对象
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);

        // 如果缓存中有 WebInfo，则直接处理
        if (webInfo != null) {
            WebInfo result = new WebInfo();
            BeanUtils.copyProperties(webInfo, result);
            result.setRandomAvatar(null);
            result.setRandomName(null);
            result.setWaifuJson(null);

            // 获取并设置访问历史统计数据
            getHistoryCache(result);
            // log.info("result:{}", result);
            return R.success(result);
        }

        // 缓存中没有 WebInfo，查询数据库
        webInfo = webInfoService.list().get(0);
        qingSiCache.put(CommonConst.WEB_INFO, webInfoService.list().get(0));
        // 设置并返回 WebInfo
        getHistoryCache(webInfo);
        return R.success(webInfo);
    }

    /**
     * 获取网站统计信息
     */
    @LoginCheck(0)
    @GetMapping("/getHistoryInfo")
    public R<Map<String, Object>> getHistoryInfo() {
        Map<String, Object> result = new HashMap<>();

        // Map<String, Object> history = (Map<String, Object>) PoetryCache.get(CommonConst.IP_HISTORY_STATISTICS);
        Map<String, Object> history = JSON.parseObject(
                qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS), new TypeReference<Map<String, Object>>() {
                });
        List<HistoryInfo> infoList = new LambdaQueryChainWrapper<>(historyInfoMapper)
                .select(HistoryInfo::getIp, HistoryInfo::getUserId, HistoryInfo::getNation, HistoryInfo::getProvince, HistoryInfo::getCity)
                .ge(HistoryInfo::getCreateTime, LocalDateTime.now().with(LocalTime.MIN))
                .list();

        result.put(CommonConst.IP_HISTORY_PROVINCE, history.get(CommonConst.IP_HISTORY_PROVINCE));
        result.put(CommonConst.IP_HISTORY_IP, history.get(CommonConst.IP_HISTORY_IP));
        result.put(CommonConst.IP_HISTORY_COUNT, history.get(CommonConst.IP_HISTORY_COUNT));
        List<Map<String, Object>> ipHistoryCount = (List<Map<String, Object>>) history.get(CommonConst.IP_HISTORY_HOUR);

        // 昨日访问数
        result.put("ip_count_yest", ipHistoryCount.stream().map(m -> m.get("ip")).distinct().count());
        result.put("username_yest", ipHistoryCount.stream().map(m -> {
            Object userId = m.get("user_id");
            if (userId != null) {
                User user = commonQuery.getUser(Integer.valueOf(userId.toString()));
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
        result.put("ip_count_today", infoList.stream().map(HistoryInfo::getIp).distinct().count());

        // 今日访问用户
        result.put("username_today", infoList.stream().map(m -> {
            Integer userId = m.getUserId();
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
                .map(HistoryInfo::getProvince).filter(Objects::nonNull)
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
        result.put(CommonConst.IP_DETAIL_TODAY,null);

        return R.success(result);
    }

    /**
     * 获取用户信息 获取赞赏
     */
    @GetMapping("/getAdmire")
    public R<List<User>> getAdmire() {
        return R.success(commonQuery.getAdmire());
    }

    /**
     * 获取看板娘消息
     */
    @GetMapping("/getWaifuJson")
    public String getWaifuJson() {

        log.info("进入看板娘消息管理");

        WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);

        // log.info("获取到的看板娘配置为：{}", webInfo.getWaifuJson());

        if (webInfo != null && StringUtils.hasText(webInfo.getWaifuJson())) {
            return webInfo.getWaifuJson();
        }
        return "{}";
    }


    /**
     * 获取缓存中的 history 数据追加到 webInfo中 为空更新缓存
     * @param webInfo
     */
    private void getHistoryCache(WebInfo webInfo) {

        Map<String, Object> historyMap = new HashMap<>();
        // 缓存中没有
        if (!StringUtils.hasText(qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS))) {
            historyMap.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
            historyMap.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
            historyMap.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
            historyMap.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount() + 1168);
            // 缓存
            qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, JSON.toJSONString(historyMap));
        }else {
            historyMap = JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS), new TypeReference<Map<String, Object>>() {});
        }


        // 设置总访问数
        webInfo.setHistoryAllCount(historyMap.getOrDefault(CommonConst.IP_HISTORY_COUNT, 1168).toString());

        // 设置24小时内访问数
        Object historyDayData = historyMap.get(CommonConst.IP_HISTORY_HOUR);
        String dayHistoryStr = Objects.isNull(historyDayData)
                ? "168"
                : String.valueOf(JSON.parseObject(historyDayData.toString(), new TypeReference<List<HistoryInfo>>() {}).size());

        webInfo.setHistoryDayCount(dayHistoryStr);
    }


    // 更新历史统计数据到缓存
    private void updateHistoryCache() {
        Map<String, Object> history = new HashMap<>();
        history.put(CommonConst.IP_HISTORY_PROVINCE, historyInfoMapper.getHistoryByProvince());
        history.put(CommonConst.IP_HISTORY_IP, historyInfoMapper.getHistoryByIp());
        history.put(CommonConst.IP_HISTORY_HOUR, historyInfoMapper.getHistoryBy24Hour());
        history.put(CommonConst.IP_HISTORY_COUNT, historyInfoMapper.getHistoryCount() + 1168);

        qingSiCache.put(CommonConst.IP_HISTORY_STATISTICS, history);
        qingSiCache.put(CommonConst.WEB_INFO, webInfoService.list().get(0));
    }
}

