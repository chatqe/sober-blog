package io.charqe.qs.modules.webInfo.controller.web;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.charqe.qs.aop.LoginCheck;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.access.service.AccessLogService;
import io.charqe.qs.modules.article.domain.dao.ArticleMapper;
import io.charqe.qs.modules.category.domain.dao.CategoryMapper;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.modules.webInfo.service.WebInfoService;
import io.charqe.qs.utils.CommonQuery;
import io.charqe.qs.utils.cache.QingSiCache;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * *
 *
 * @author Joy
 * @since 2026-01-17 10:47
 */
@Slf4j
@RestController
@RequestMapping("/webInfo")
@Tag(name = "webInfo", description = "网站信息管理")
public class WebInfoController {

    @Value("${store.type}")
    private String defaultType;

    @Resource
    private WebInfoService webInfoService;



    @Resource
    private AccessLogService accessLogService;

    @Resource
    private CommonQuery commonQuery;

    @Resource
    private QingSiCache qingSiCache;




    /**
     * 更新网站信息
     *
     * @param webInfo 网站信息
     * @return 更新结果
     */
    @Operation(summary = "更新网站信息", description = "用于管理员更新网站信息，包括网站标题、描述、Logo等")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @ApiResponse(responseCode = "401", description = "未授权，需要管理员权限")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @LoginCheck(0)
    @PostMapping("/update")
    public R<WebInfo> updateWebInfo(
            @Parameter(name = "webInfo", description = "网站信息对象", required = true, example = "{\"webName\":\"清思博客\",\"webDescription\":\"一个技术博客网站\"}")
            @RequestBody WebInfo webInfo) {
        webInfoService.updateById(webInfo);

        LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoService.getBaseMapper());
        List<WebInfo> list = wrapper.list();
        if (!CollectionUtils.isEmpty(list)) {
            list.get(0).setDefaultStoreType(defaultType);
            qingSiCache.put(CommonConst.WEB_INFO, list.get(0));
        }
        return R.success();
    }


    /**
     * 获取网站信息
     *
     * @return 网站信息
     */
    @Operation(summary = "获取网站信息", description = "用于获取网站的基本信息，包括网站标题、描述、Logo等")
    @ApiResponse(responseCode = "200", description = "查询成功，返回网站信息")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/get")
    public R<WebInfo> getWebInfo() {
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
     * 获取赞赏用户信息
     *
     * @return 赞赏用户列表
     */
    @Operation(summary = "获取赞赏用户信息", description = "用于获取赞赏用户列表")
    @ApiResponse(responseCode = "200", description = "查询成功，返回赞赏用户列表")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/getAdmire")
    public R<List<User>> getAdmire() {
        return R.success(commonQuery.getAdmire());
    }

    /**
     * 获取看板娘消息
     *
     * @return 看板娘消息JSON字符串
     */
    @Operation(summary = "获取看板娘消息", description = "用于获取网站看板娘的配置信息，返回JSON格式的配置数据")
    @ApiResponse(responseCode = "200", description = "查询成功，返回看板娘配置JSON")
    @ApiResponse(responseCode = "500", description = "服务器内部错误")
    @GetMapping("/getWaifuJson")
    public String getWaifuJson() {

        log.info("进入看板娘消息管理");

        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);

        if (webInfo != null && StringUtils.hasText(webInfo.getWaifuJson())) {
            return webInfo.getWaifuJson();
        }
        return "{}";
    }


    /**
     * 获取缓存中的 history 数据追加到 webInfo中 为空更新缓存
     *
     * @param webInfo
     */
    private void getHistoryCache(WebInfo webInfo) {

        Map<String, Object> ipStatisticsMap = new HashMap<>();
        // 缓存中没有
        if (!StringUtils.hasText(qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS))) {
            accessLogService.addIPCache(ipStatisticsMap);
        } else {
            ipStatisticsMap = JSON.parseObject(qingSiCache.get(CommonConst.IP_HISTORY_STATISTICS), new TypeReference<Map<String, Object>>() {
            });
        }


        // 设置总访问数
        webInfo.setHistoryAllCount(ipStatisticsMap.getOrDefault(CommonConst.IP_HISTORY_COUNT, 1168).toString());

        // 设置24小时内访问数
        Object historyDayData = ipStatisticsMap.get(CommonConst.IP_HISTORY_HOUR);
        String dayHistoryStr = Objects.isNull(historyDayData)
                ? "168"
                : String.valueOf(((List<Map<String, Object>>) historyDayData).size());

        webInfo.setHistoryDayCount(dayHistoryStr);
    }


    // 更新历史统计数据到缓存
    private void updateHistoryCache() {
        Map<String, Object> ipStatisticsMap = new HashMap<>();
        accessLogService.addIPCache(ipStatisticsMap);
        qingSiCache.put(CommonConst.WEB_INFO, webInfoService.list().get(0));
    }
}
