package com.joy.qs.utils.storage;

import com.joy.qs.constants.CommonConst;
import com.joy.qs.model.user.pojo.User;
import com.joy.qs.utils.BlogUtil;
import com.joy.qs.utils.cache.PoetryCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

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

    public boolean doFilterFile(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        log.info("=================> 进入FileFilter过滤器处理,URL:{}",httpServletRequest.getRequestURI());
        if (matcher.match("/resource/upload", httpServletRequest.getRequestURI())) {
            // 根据自定义的请求头 Authorization 获取是否进行缓存用户信息
            String token = BlogUtil.getToken();
            boolean hasText = StringUtils.hasText(token);
            if (hasText) {
                User user = (User) PoetryCache.get(token);

                if (user != null) {

                    if (user.getId().intValue() == BlogUtil.getAdminUser().getId().intValue()) {
                        return false;
                    }

                    AtomicInteger atomicInteger = (AtomicInteger) PoetryCache.get(CommonConst.SAVE_COUNT_USER_ID + user.getId().toString());
                    if (atomicInteger == null) {
                        atomicInteger = new AtomicInteger();
                        PoetryCache.put(CommonConst.SAVE_COUNT_USER_ID + user.getId().toString(), atomicInteger, CommonConst.SAVE_EXPIRE);
                    }
                    int userIdCount = atomicInteger.getAndIncrement();

                    String ip = BlogUtil.getIpAddr(BlogUtil.getRequest());
                    AtomicInteger atomic = (AtomicInteger) PoetryCache.get(CommonConst.SAVE_COUNT_IP + ip);
                    if (atomic == null) {
                        atomic = new AtomicInteger();
                        PoetryCache.put(CommonConst.SAVE_COUNT_IP + ip, atomic, CommonConst.SAVE_EXPIRE);
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
