package io.charqe.qs.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Bean 中转站
 * 在启动时把需要的服务 静态保存 下来，工具类后面就能随意使用。
 *
 * @author sjq
 * @since 2025-12-01 19:59
 */
@Component   // 1. 让 Spring 管理这个 Bean
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;   // 2. 静态保存容器引用
    }

    /**
     * 按类型获取 Bean（工具类里直接调用）
     */
    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}


