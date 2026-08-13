package com.qs.controller;

import com.qs.entity.User;
import com.qs.enums.MenuCode;
import com.qs.service.DeliveryService;
import com.qs.service.PartnerService;
import com.qs.service.PermissionService;
import com.qs.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final DeliveryService deliveryService;
    private final PartnerService partnerService;

    public UserController(UserService userService, PermissionService permissionService,
                          DeliveryService deliveryService, PartnerService partnerService) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.deliveryService = deliveryService;
        this.partnerService = partnerService;
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
        model.addAttribute("partners", partnerService.listAll());
        model.addAttribute("activeTab", "users");
        return "user/form";
    }

    @GetMapping("/{id}/permissions")
    public String permissionsForm(@PathVariable String id, Model model,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        addUserToModel(model, userDetails);
        User user = userService.getById(id);
        model.addAttribute("user", user);
        model.addAttribute("allMenus", MenuCode.allMenus());
        model.addAttribute("selectedMenus", permissionService.getMenuCodes(user.getId()));
        model.addAttribute("deliveries", deliveryService.listOptions());
        model.addAttribute("selectedDeliveries", permissionService.getAssignedDeliveryIds(user.getId()));
        model.addAttribute("activeTab", "users");
        return "user/permissions";
    }

    @PostMapping("/{id}/permissions")
    public String savePermissions(@PathVariable String id,
                                  @RequestParam(required = false) List<String> menus,
                                  @RequestParam(required = false) List<String> deliveries,
                                  RedirectAttributes redirectAttributes) {
        try {
            permissionService.savePermissions(id, menus, deliveries);
            redirectAttributes.addFlashAttribute("success", "权限已保存");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/users/" + id + "/permissions";
        }
        return "redirect:/users";
    }

    @PostMapping("/save")
    public String save(@RequestParam String id,
                       @RequestParam String displayName,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       @RequestParam(required = false) String newPassword,
                       @RequestParam(required = false) String partnerId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       RedirectAttributes redirectAttributes) {
        try {
            userService.updateUser(id, displayName, enabled, newPassword, partnerId, userDetails.getUsername());
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
