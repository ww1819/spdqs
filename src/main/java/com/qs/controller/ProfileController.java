package com.qs.controller;

import com.qs.entity.User;
import com.qs.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user.getDisplayName());
        model.addAttribute("currentUsername", userDetails.getUsername());
        return "user/profile";
    }

    @PostMapping("/profile/save")
    public String save(@RequestParam String displayName,
                       @RequestParam(required = false) String newPassword,
                       @RequestParam(required = false) String confirmPassword,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(userDetails.getUsername(), displayName, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "个人信息已保存");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
