package com.qs.config;

import com.qs.service.UserService;
import com.qs.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class DataInitializer {

    @Bean
    @Order(1)
    CommandLineRunner initDefaultUser(UserService userService, PermissionService permissionService) {
        return args -> {
            userService.createUserIfAbsent("王威", "王威", "wangwei");
            permissionService.ensureBootstrapPermissions();
            // 确保管理员始终有账号管理权限与全部医院授权
            var admin = userService.findByUsername("王威");
            if (admin != null) {
                permissionService.grantAllMenus(admin.getId());
                permissionService.grantAllArchives(admin.getId());
            }
        };
    }
}
