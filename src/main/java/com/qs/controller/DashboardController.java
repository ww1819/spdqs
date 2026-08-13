package com.qs.controller;

import com.qs.service.DashboardService;
import com.qs.service.PermissionService;
import com.qs.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Set;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;
    private final PermissionService permissionService;

    public DashboardController(DashboardService dashboardService, UserService userService,
                               PermissionService permissionService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping
    public String index(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String currentUser = resolveDisplayName(userDetails);
        String username = userDetails != null ? userDetails.getUsername() : "";
        Set<String> allowedArchives = permissionService.getAllowedDeliveryIds(username);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("returnUrl", "/dashboard");
        model.addAttribute("dashboard", dashboardService.build(currentUser, allowedArchives));
        model.addAttribute("activeTab", "dashboard");
        return "dashboard/index";
    }

    private String resolveDisplayName(UserDetails userDetails) {
        if (userDetails == null) {
            return "";
        }
        var user = userService.findByUsername(userDetails.getUsername());
        return user != null ? user.getDisplayName() : userDetails.getUsername();
    }
}
