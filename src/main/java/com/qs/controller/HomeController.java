package com.qs.controller;

import com.qs.enums.MenuCode;
import com.qs.service.PermissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;

@Controller
public class HomeController {

    private final PermissionService permissionService;

    public HomeController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Set<String> menus = permissionService.getMenuCodesByUsername(userDetails.getUsername());
        if (menus.contains(MenuCode.DASHBOARD.getCode())) {
            return "redirect:/dashboard";
        }
        for (MenuCode menu : MenuCode.allMenus()) {
            if (menus.contains(menu.getCode())) {
                return "redirect:" + menu.getPathPrefix();
            }
        }
        return "redirect:/profile";
    }
}
