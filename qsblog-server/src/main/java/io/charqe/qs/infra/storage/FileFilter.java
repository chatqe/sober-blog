package io.charqe.qs.infra.storage;

import com.alibaba.fastjson2.JSON;
import io.charqe.qs.common.constants.CommonConst;
import io.charqe.qs.modules.user.domain.pojo.User;
import io.charqe.qs.modules.user.auth.AuthSessionService;
import io.charqe.qs.modules.user.auth.SessionScope;
import io.charqe.qs.utils.BlogUtil;
import io.charqe.qs.utils.IPUtil;
import io.charqe.qs.utils.cache.QingSiCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 文件过滤器
 *
 * @author Upjqs
 * @since 2024-08-19 10:43
 */

@Slf4j
@Component
public class FileFilter {

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Resource
    private QingSiCache qingSiCache;

    @Resource
    private AuthSessionService sessions;

    public boolean doFilterFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        log.info("=================> 进入FileFilter过滤器处理,URL:{}", httpServletRequest.getRequestURI());
        if (matcher.match("/file/admin/upload", httpServletRequest.getRequestURI())) {
            // 根据自定义的请求头 Authorization 获取是否进行缓存用户信息
            String token = httpServletRequest.getHeader("Authorization");
            User identity = sessions.resolve(token, SessionScope.ADMIN, false).user();
            boolean hasText = StringUtils.hasText(token);
            if (hasText) {
                User user = identity;

                if (user != null) {

                    if (user.getId().intValue() == BlogUtil.getAdminUser().getId().intValue()) {
                        return false;
                    }

                    AtomicInteger atomicInteger = JSON.parseObject(qingSiCache.get(CommonConst.SAVE_COUNT_USER_ID + user.getId().toString()), AtomicInteger.class);
                    if (atomicInteger == null) {
                        atomicInteger = new AtomicInteger();
                        qingSiCache.put(CommonConst.SAVE_COUNT_USER_ID + user.getId().toString(), atomicInteger, CommonConst.SAVE_EXPIRE);
                    }
                    int userIdCount = atomicInteger.getAndIncrement();

                    String ip = IPUtil.getRealIp(IPUtil.getRequest());
                    AtomicInteger atomic = JSON.parseObject(qingSiCache.get(CommonConst.SAVE_COUNT_IP + ip), AtomicInteger.class);
                    if (atomic == null) {
                        atomic = new AtomicInteger();
                        qingSiCache.put(CommonConst.SAVE_COUNT_IP + ip, atomic, CommonConst.SAVE_EXPIRE);
                    }
                    int ipCount = atomic.getAndIncrement();

                    return userIdCount >= CommonConst.SAVE_MAX_COUNT || ipCount >= CommonConst.SAVE_MAX_COUNT;
                }

                // 过滤器中获取不到用户信息，直接返回true
                log.info("=================> 缓存中用户信息为null 未获取到用户信息，直接返回true");

            }
            return true;
        } else {
            return false;
        }
    }
}


