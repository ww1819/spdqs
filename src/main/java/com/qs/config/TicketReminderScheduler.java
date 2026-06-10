package com.qs.config;

import com.qs.service.ReminderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketReminderScheduler {

    private final ReminderService reminderService;

    public TicketReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(cron = "0 0 15,16,17,18 * * ?")
    public void sendDueReminders() {
        int hour = java.time.LocalTime.now().getHour();
        reminderService.processDueReminders(hour);
    }
}
