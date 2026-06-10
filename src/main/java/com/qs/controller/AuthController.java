package com.qs.controller;

import com.qs.config.RememberLoginSuccessHandler;
import com.qs.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Validated
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        String savedUsername = null;
        String savedPassword = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RememberLoginSuccessHandler.COOKIE_USER.equals(cookie.getName())) {
                    savedUsername = RememberLoginSuccessHandler.decode(cookie.getValue());
                } else if (RememberLoginSuccessHandler.COOKIE_PASS.equals(cookie.getName())) {
                    savedPassword = RememberLoginSuccessHandler.decode(cookie.getValue());
                }
            }
        }
        if (savedUsername != null && !savedUsername.isBlank()) {
            model.addAttribute("savedUsername", savedUsername);
            model.addAttribute("savedPassword", savedPassword);
            model.addAttribute("rememberChecked", true);
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("form") RegisterForm form, RedirectAttributes redirectAttributes) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            redirectAttributes.addFlashAttribute("error", "两次输入的密码不一致");
            return "redirect:/register";
        }
        try {
            userService.register(form.getUsername(), form.getDisplayName(), form.getPassword());
            redirectAttributes.addFlashAttribute("success", "注册成功，账号待审核，请联系管理员启用后再登录");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }

    public static class RegisterForm {

        @NotBlank(message = "账户名不能为空")
        private String username;

        @NotBlank(message = "显示名不能为空")
        private String displayName;

        @NotBlank(message = "密码不能为空")
        @Size(min = 4, message = "密码至少4位")
        private String password;

        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
