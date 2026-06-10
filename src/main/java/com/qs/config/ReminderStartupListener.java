package com.qs.config;

import com.qs.service.ReminderService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ReminderStartupListener {

    private final ReminderService reminderService;

    public ReminderStartupListener(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            reminderService.catchUpMissedRemindersToday();
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ReminderStartupListener.class)
                    .error("启动时补跑提醒失败（若刚升级请检查数据库脚本是否已执行）: {}", ex.getMessage());
        }
    }
}
