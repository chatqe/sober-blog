package io.charqe.qs.aop;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.common.exception.BizException;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.IPUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


@Aspect
@Component
@Order(1)
@Slf4j
public class SaveCheckAspect {

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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

                String userKey = CommonConst.SAVE_COUNT_USER_ID + user.getId().toString();
                Long userIdCount = stringRedisTemplate.opsForValue().increment(userKey, 1);
                // 首次设置过期时间（24小时）
                if (userIdCount != null && userIdCount == 1) {
                    stringRedisTemplate.expire(userKey, CommonConst.SAVE_EXPIRE, TimeUnit.SECONDS);
                }
                if (userIdCount != null && userIdCount > CommonConst.SAVE_MAX_COUNT) {
                    log.info("用户保存超限：{}，次数：{}", user.getId(), userIdCount);
                    flag = true;
                }
            }
        }

        String ip = IPUtil.getRealIp(IPUtil.getRequest());
        String ipKey = CommonConst.SAVE_COUNT_IP + ip;
        Long ipCount = stringRedisTemplate.opsForValue().increment(ipKey, 1);
        if (ipCount != null && ipCount == 1) {
            stringRedisTemplate.expire(ipKey, CommonConst.SAVE_EXPIRE, TimeUnit.SECONDS);
        }
        if (ipCount != null && ipCount > CommonConst.SAVE_MAX_COUNT) {
            log.info("IP保存超限：{}，次数：{}", ip, ipCount);
            flag = true;
        }

        if (flag) {
            throw new BizException("今日提交次数已用尽，请一天后再来！");
        }

        return joinPoint.proceed();
    }
}


