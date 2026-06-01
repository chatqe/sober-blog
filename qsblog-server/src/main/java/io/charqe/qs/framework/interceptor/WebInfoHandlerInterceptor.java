package io.charqe.qs.framework.interceptor;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.common.model.R;
import io.charqe.qs.modules.user.controller.web.dto.LoginUserInfo;
import io.charqe.qs.modules.webInfo.domain.pojo.WebInfo;
import io.charqe.qs.utils.JWTUtils;
import io.charqe.qs.utils.UserContext;
import io.charqe.qs.utils.cache.QingSiCache;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

@Component
public class WebInfoHandlerInterceptor implements HandlerInterceptor {

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private JWTUtils jwtUtils;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (StrUtil.isNotBlank( token) && jwtUtils.validateToken(token)) {
                LoginUserInfo userInfo = jwtUtils.parseUser(token);
                if (Objects.nonNull(userInfo)) {
                    UserContext.set(userInfo);
                }
            }


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
        UserContext.clear();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}


