package com.qs.config;

import com.qs.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner initDefaultUser(UserService userService) {
        return args -> userService.createUserIfAbsent("王威", "王威", "wangwei");
    }
}
