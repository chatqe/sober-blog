package com.ld.poetry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
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
                        "/user/login",
                        "/admin/**",
                        "/webInfo/getWebInfo",
                        "/webInfo/updateWebInfo");
    }
}
