package com.ld.poetry.config;

import com.alibaba.fastjson.JSON;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.enums.CodeMsg;
import com.ld.poetry.utils.RedisUtil;
import com.ld.poetry.utils.cache.QingSiCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class WebInfoHandlerInterceptor implements HandlerInterceptor {

    @Resource
    // @Autowired
    private QingSiCache qingSiCache;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        WebInfo webInfo = JSON.parseObject(qingSiCache.get(CommonConst.WEB_INFO), WebInfo.class);
        if (webInfo == null || !webInfo.getStatus()) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(PoetryResult.fail(CodeMsg.SYSTEM_REPAIR.getCode(), CodeMsg.SYSTEM_REPAIR.getMsg())));
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
