package io.charqe.qs.common.config;

import io.charqe.qs.framework.interceptor.WebInfoHandlerInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebInfoConfigurer implements WebMvcConfigurer {

    @Resource
    private WebInfoHandlerInterceptor webInfoHandlerInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInfoHandlerInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/user/login",
                        "/admin/**",
                        "/webInfo/getWebInfo",
                        "/webInfo/updateWebInfo");
    }
}


