package com.qs.controller;

import com.qs.entity.User;
import com.qs.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("users", userService.listAll());
        model.addAttribute("activeTab", "users");
        return "user/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        model.addAttribute("user", userService.getById(id));
        model.addAttribute("activeTab", "users");
        return "user/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam String id,
                       @RequestParam String displayName,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       @RequestParam(required = false) String newPassword,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, displayName, enabled, newPassword, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "账号信息已保存");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/users/" + id + "/edit";
        }
        return "redirect:/users";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable String id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            userService.setEnabled(id, true, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "账号已启用");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable String id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            userService.setEnabled(id, false, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("success", "账号已停用");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/users";
    }

    private void addUserToModel(Model model, UserDetails userDetails) {
        if (userDetails != null) {
            User user = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", user != null ? user.getDisplayName() : userDetails.getUsername());
            model.addAttribute("currentUsername", userDetails.getUsername());
        }
    }
}
