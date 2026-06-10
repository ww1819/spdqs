package com.qs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccountStatusInterceptor accountStatusInterceptor;

    public WebMvcConfig(AccountStatusInterceptor accountStatusInterceptor) {
        this.accountStatusInterceptor = accountStatusInterceptor;
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
    }
}
