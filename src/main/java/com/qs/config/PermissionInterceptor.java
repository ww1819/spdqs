package com.qs.config;

import com.qs.enums.MenuCode;
import com.qs.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }

        MenuCode required = resolveRequiredMenu(path);
        if (required == null) {
            return true;
        }

        String username = resolveUsername(auth);
        if (permissionService.hasMenu(username, required)) {
            return true;
        }

        if (path.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"无权限访问\"}");
            return false;
        }

        String fallback = firstAllowedPath(username);
        response.sendRedirect(request.getContextPath() + fallback + "?denied=1");
        return false;
    }

    private MenuCode resolveRequiredMenu(String path) {
        if (path.startsWith("/dashboard")) {
            return MenuCode.DASHBOARD;
        }
        if (path.startsWith("/archives") || path.startsWith("/api/archives")) {
            return MenuCode.ARCHIVES;
        }
        if (path.startsWith("/customers")) {
            return MenuCode.CUSTOMERS;
        }
        if (path.startsWith("/products")) {
            return MenuCode.PRODUCTS;
        }
        if (path.startsWith("/partners")) {
            return MenuCode.PARTNERS;
        }
        if (path.startsWith("/tickets") || path.startsWith("/api/tickets")) {
            return MenuCode.TICKETS;
        }
        if (path.startsWith("/analysis") || path.startsWith("/api/analysis")) {
            return MenuCode.ANALYSIS;
        }
        if (path.startsWith("/users")) {
            return MenuCode.USERS;
        }
        return null;
    }

    private String firstAllowedPath(String username) {
        Set<String> menus = permissionService.getMenuCodesByUsername(username);
        for (MenuCode menu : MenuCode.allMenus()) {
            if (menus.contains(menu.getCode())) {
                return menu.getPathPrefix();
            }
        }
        return "/profile";
    }

    private String resolveUsername(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return auth.getName();
    }
}
