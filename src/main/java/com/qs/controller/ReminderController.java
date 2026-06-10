package com.qs.controller;

import com.qs.service.ReminderService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/{id}/read")
    public String markRead(@PathVariable String id,
                           @RequestParam(required = false, defaultValue = "/archives") String returnUrl) {
        reminderService.markRead(id);
        if (!returnUrl.startsWith("/")) {
            returnUrl = "/archives";
        }
        return "redirect:" + returnUrl;
    }
}
