package com.project.his.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.project.his.interceptor.LoginCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 注册Sa-Token的路由拦截器，负责Token校验和自动续期
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/register", "/auth/login", "/auth/email", "/auth/IsExists", "/admin/schedule/auto");

       //注册自定义拦截器对象
        registry.addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/register")
                .excludePathPatterns("/auth/login")
                .excludePathPatterns("/auth/email")
                .excludePathPatterns("/auth/IsExists")
                .excludePathPatterns("/admin/schedule/auto");
    }
}
