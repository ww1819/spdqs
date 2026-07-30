package com.qs.config;

import com.qs.enums.MenuCode;
import com.qs.service.PermissionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Component
public class RememberLoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String COOKIE_USER = "qs_remember_user";
    public static final String COOKIE_PASS = "qs_remember_pass";
    public static final String PARAM_REMEMBER = "rememberMe";

    private static final int MAX_AGE_SECONDS = (int) Duration.ofDays(30).toSeconds();

    private final PermissionService permissionService;

    public RememberLoginSuccessHandler(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (isRememberRequested(request)) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            addCookie(response, COOKIE_USER, username, MAX_AGE_SECONDS);
            addCookie(response, COOKIE_PASS, password, MAX_AGE_SECONDS);
        } else {
            clearCookie(response, COOKIE_USER);
            clearCookie(response, COOKIE_PASS);
        }
        response.sendRedirect(request.getContextPath() + resolveHomePath(authentication));
    }

    private String resolveHomePath(Authentication authentication) {
        String username = authentication.getName();
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        }
        Set<String> menus = permissionService.getMenuCodesByUsername(username);
        if (menus.contains(MenuCode.DASHBOARD.getCode())) {
            return "/dashboard";
        }
        for (MenuCode menu : MenuCode.allMenus()) {
            if (menus.contains(menu.getCode())) {
                return menu.getPathPrefix();
            }
        }
        return "/profile";
    }

    static boolean isRememberRequested(HttpServletRequest request) {
        String remember = request.getParameter(PARAM_REMEMBER);
        return remember != null && !remember.isBlank();
    }

    static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, encode(value));
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }

    static void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
    }

    static String encode(String value) {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String decode(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
