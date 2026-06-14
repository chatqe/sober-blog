package io.charqe.qs.aop;

import io.charqe.qs.common.exception.ServiceException;
import io.charqe.qs.utils.IPUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等切面
 *
 * @author sjq
 * @since 2025-11-30 15:27
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotentAspect {

    // private final RedisTemplate<String, String> redisTemplate;

    private final RedisTemplate<String, String> redisTemplate;
    @Resource
    private QingSiCache qingSiCache;
    private static final String PREFIX = "idempotent";   // 全局前缀

    @Around("@annotation(idem)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idem) throws Throwable {
        String key = buildKey(pjp, idem);
        Boolean exist = qingSiCache.putIfAbsent(key, idem.ttl(), TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(exist)) {
            throw new ServiceException(idem.message());
        }
        // 继续执行方法
        return pjp.proceed();
    }

    /**
     * 通过 SpEL 解析注解中配置的 key() 表达式，得到本次请求的维度值
     * 例："#userId"、"#ip+':'+#phone"、"#orderNo"
     *
     * @param pjp  Spring AOP 对 "正在执行的方法" 的一个运行时句柄。拿到它，就等于拿到了 方法元数据 + 运行时参数 + 控制权，可以在切面里"为所欲为"——记录日志、鉴权、限流、改参数、改返回值，甚至决定是否继续执行。
     * @param idem
     * @return
     */
    private String buildKey(ProceedingJoinPoint pjp, Idempotent idem) {
        // 创建 SpEL 上下文，把方法参数按名字放进去，供 #xxx 读取
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        // 拿到方法级别的元数据。方法名 参数名 参数类型 返回类型 注解数组等等
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        String[] names = ms.getParameterNames();
        Object[] args = pjp.getArgs(); // 参数值
        for (int i = 0; i < names.length; i++) {
            ctx.setVariable(names[i], args[i]);
        }
        // 把 IP 也放进去，供 #ip 使用
        ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 判空防御 非 Web 环境可能为 null
        if (attr != null) {
            ctx.setVariable("ip", IPUtil.getRealIp(attr.getRequest()));
        }
        // SpEL 编译器，负责把字符串公式变成可运行的代码对象
        ExpressionParser parser = new SpelExpressionParser();
        // 使用注解中配置的 key() 表达式，默认为 "#ip"
        String spelExpression = idem.key();
        if (spelExpression == null || spelExpression.isEmpty()) {
            spelExpression = "#ip";
        }
        String dimension = parser.parseExpression(spelExpression).getValue(ctx, String.class);
        // key：前缀:方法名:维度值  例：idempotent:createOrder:123456
        return String.join(":", PREFIX, ms.getName(), dimension);
    }
}

