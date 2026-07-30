package com.qs.config;

import com.qs.service.PermissionService;
import com.qs.service.ReminderService;
import com.qs.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ReminderAdvice {

    private final ReminderService reminderService;
    private final UserService userService;
    private final PermissionService permissionService;

    public ReminderAdvice(ReminderService reminderService, UserService userService,
                          PermissionService permissionService) {
        this.reminderService = reminderService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @ModelAttribute
    public void addReminders(Authentication authentication, org.springframework.ui.Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }
        String displayName = resolveDisplayName(authentication);
        model.addAttribute("reminders", reminderService.listUnread(displayName));

        String username = resolveUsername(authentication);
        model.addAttribute("allowedMenus", permissionService.getMenuCodesByUsername(username));
    }

    private String resolveDisplayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            var user = userService.findByUsername(userDetails.getUsername());
            return user != null ? user.getDisplayName() : userDetails.getUsername();
        }
        return authentication.getName();
    }

    private String resolveUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName();
    }
}
