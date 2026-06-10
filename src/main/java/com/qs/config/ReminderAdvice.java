package com.qs.config;

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

    public ReminderAdvice(ReminderService reminderService, UserService userService) {
        this.reminderService = reminderService;
        this.userService = userService;
    }

    @ModelAttribute
    public void addReminders(Authentication authentication, org.springframework.ui.Model model) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return;
        }
        String displayName = resolveDisplayName(authentication);
        model.addAttribute("reminders", reminderService.listUnread(displayName));
    }

    private String resolveDisplayName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            var user = userService.findByUsername(userDetails.getUsername());
            return user != null ? user.getDisplayName() : userDetails.getUsername();
        }
        return authentication.getName();
    }
}
