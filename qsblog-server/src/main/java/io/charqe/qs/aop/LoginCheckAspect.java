package io.charqe.qs.aop;

import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.AuthAccessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Aspect
@Component
@Order(0)
@Slf4j
public class LoginCheckAspect {
    @Resource
    private AuthSessionService sessions;

    @Around("@annotation(loginCheck)")
    public Object around(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        AuthSessionService.Resolved identity = sessions.current(loginCheck.scope(), false);
        if (identity.user().getUserType() > loginCheck.value()) {
            throw new AuthAccessException(403, "权限不足");
        }
        sessions.current(loginCheck.scope(), loginCheck.activity());
        return joinPoint.proceed();
    }
}


