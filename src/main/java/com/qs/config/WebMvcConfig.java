package com.qs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccountStatusInterceptor accountStatusInterceptor;
    private final PermissionInterceptor permissionInterceptor;

    public WebMvcConfig(AccountStatusInterceptor accountStatusInterceptor,
                        PermissionInterceptor permissionInterceptor) {
        this.accountStatusInterceptor = accountStatusInterceptor;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accountStatusInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/logout",
                        "/css/**",
                        "/js/**",
                        "/favicon.ico",
                        "/favicon.svg",
                        "/error"
                );
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/logout",
                        "/profile",
                        "/profile/**",
                        "/css/**",
                        "/js/**",
                        "/favicon.ico",
                        "/favicon.svg",
                        "/error"
                );
    }
}
