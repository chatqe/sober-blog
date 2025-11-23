package com.joy.qs.config;

import com.alibaba.fastjson.JSON;
import com.joy.qs.common.R;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.model.webinfo.pojo.WebInfo;
import com.joy.qs.enums.CodeMsg;
import com.joy.qs.utils.cache.QingSiCache;
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
            response.getWriter().write(JSON.toJSONString(R.fail(CodeMsg.SYSTEM_REPAIR.getCode(), CodeMsg.SYSTEM_REPAIR.getMsg())));
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
