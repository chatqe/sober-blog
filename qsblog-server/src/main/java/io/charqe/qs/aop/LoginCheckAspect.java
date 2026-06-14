package io.charqe.qs.aop;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.enums.CodeMsg;
import io.charqe.qs.modules.user.enums.UserTypeEnum;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.common.exception.LoginException;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.IPUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Aspect
@Component
@Order(0)
@Slf4j
public class LoginCheckAspect {
    @Resource
    private QingSiCache qingSiCache;

    @Around("@annotation(loginCheck)")
    public Object around(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        String token = BlogUtil.getToken();
        if (!StringUtils.hasText(token)) {
            throw new LoginException(CodeMsg.NOT_LOGIN.getMsg());
        }

        // User user = (User) PoetryCache.get(token);
        User user = JSON.parseObject(qingSiCache.get(token), User.class);

        if (user == null) {
            throw new LoginException(CodeMsg.LOGIN_EXPIRED.getMsg());
        }

        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            if (loginCheck.value() == UserTypeEnum.ADMIN.code() || loginCheck.value() == UserTypeEnum.DEV.code()) {
                throw new LoginException("请输入管理员账号！");
            }
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            log.debug("管理员请求IP：{}", IPUtil.getRealIp(IPUtil.getRequest()));
            if (loginCheck.value() == UserTypeEnum.ADMIN.code() && user.getId().intValue() != CommonConst.ADMIN_USER_ID) {
                throw new LoginException("请输入管理员账号！");
            }
        } else {
            throw new LoginException(CodeMsg.NOT_LOGIN.getMsg());
        }

        if (loginCheck.value() < user.getUserType()) {
            throw new BizException("权限不足！");
        }

        //重置过期时间
        String userId = user.getId().toString();
        boolean flag1 = false;
        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            // flag1 = PoetryCache.get(CommonConst.USER_TOKEN_INTERVAL + userId) == null;
            flag1 = qingSiCache.get(CommonConst.USER_TOKEN_INTERVAL + userId) == null;
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            // flag1 = PoetryCache.get(CommonConst.ADMIN_TOKEN_INTERVAL + userId) == null;
            flag1 = qingSiCache.get(CommonConst.ADMIN_TOKEN_INTERVAL + userId) == null;
        }

        if (flag1) {
            synchronized (userId.intern()) {
                boolean flag2 = false;
                if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
                    // flag2 = PoetryCache.get(CommonConst.USER_TOKEN_INTERVAL + userId) == null;
                    flag2 = qingSiCache.get(CommonConst.USER_TOKEN_INTERVAL + userId) == null;
                } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
                    // flag2 = PoetryCache.get(CommonConst.ADMIN_TOKEN_INTERVAL + userId) == null;
                    flag2 = qingSiCache.get(CommonConst.ADMIN_TOKEN_INTERVAL + userId) == null;
                }

                if (flag2) {
                    // PoetryCache.put(token, user, CommonConst.TOKEN_EXPIRE);
                    qingSiCache.put(token, user, CommonConst.TOKEN_EXPIRE);
                    if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
                        // PoetryCache.put(CommonConst.USER_TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                        // PoetryCache.put(CommonConst.USER_TOKEN_INTERVAL + userId, token, CommonConst.TOKEN_INTERVAL);
                        qingSiCache.put(CommonConst.USER_TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                        qingSiCache.put(CommonConst.USER_TOKEN_INTERVAL + userId, token, CommonConst.TOKEN_INTERVAL);
                    } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
                        // PoetryCache.put(CommonConst.ADMIN_TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                        // PoetryCache.put(CommonConst.ADMIN_TOKEN_INTERVAL + userId, token, CommonConst.TOKEN_INTERVAL);
                        qingSiCache.put(CommonConst.ADMIN_TOKEN + userId, token, CommonConst.TOKEN_EXPIRE);
                        qingSiCache.put(CommonConst.ADMIN_TOKEN_INTERVAL + userId, token, CommonConst.TOKEN_INTERVAL);
                    }
                }
            }
        }
        return joinPoint.proceed();
    }
}


