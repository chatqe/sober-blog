package com.joy.qs.aop;

import com.alibaba.fastjson2.JSON;
import com.joy.qs.constants.CommonConst;
import com.joy.qs.handle.BlogRuntimeException;
import com.joy.qs.model.user.pojo.User;
import com.joy.qs.utils.BlogUtil;
import com.joy.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicInteger;


@Aspect
@Component
@Order(1)
@Slf4j
public class SaveCheckAspect {

    @Resource
    private QingSiCache qingSiCache;

    @Around("@annotation(saveCheck)")
    public Object around(ProceedingJoinPoint joinPoint, SaveCheck saveCheck) throws Throwable {
        boolean flag = false;

        String token = BlogUtil.getToken();
        if (StringUtils.hasText(token)) {
            User user = JSON.parseObject(qingSiCache.get(token), User.class);
            if (user != null) {
                if (user.getId().intValue() == BlogUtil.getAdminUser().getId().intValue()) {
                    return joinPoint.proceed();
                }

                AtomicInteger atomicInteger = JSON.parseObject(qingSiCache.get(CommonConst.SAVE_COUNT_USER_ID + user.getId().toString()), AtomicInteger.class);
                if (atomicInteger == null) {
                    atomicInteger = new AtomicInteger();
                    qingSiCache.put(CommonConst.SAVE_COUNT_USER_ID + user.getId().toString(), atomicInteger, CommonConst.SAVE_EXPIRE);
                }
                int userIdCount = atomicInteger.getAndIncrement();
                if (userIdCount >= CommonConst.SAVE_MAX_COUNT) {
                    log.info("用户保存超限：" + user.getId().toString() + "，次数：" + userIdCount);
                    flag = true;
                }
            }
        }

        String ip = BlogUtil.getIpAddr(BlogUtil.getRequest());
        AtomicInteger atomic = JSON.parseObject(qingSiCache.get(CommonConst.SAVE_COUNT_IP + ip), AtomicInteger.class);
        if (atomic == null) {
            atomic = new AtomicInteger();
            qingSiCache.put(CommonConst.SAVE_COUNT_IP + ip, atomic, CommonConst.SAVE_EXPIRE);
        }
        int ipCount = atomic.getAndIncrement();
        if (ipCount > CommonConst.SAVE_MAX_COUNT) {
            log.info("IP保存超限：" + ip + "，次数：" + ipCount);
            flag = true;
        }

        if (flag) {
            throw new BlogRuntimeException("今日提交次数已用尽，请一天后再来！");
        }

        return joinPoint.proceed();
    }
}
