package com.qs.service;

import com.qs.entity.Reminder;
import com.qs.entity.Ticket;
import com.qs.enums.TicketStatus;
import com.qs.repository.ReminderRepository;
import com.qs.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReminderService {

    private static final List<Integer> REMIND_HOURS = List.of(15, 16, 17, 18);
    private static final List<Integer> REMIND_DAYS_BEFORE = List.of(0, 1, 3);

    private final ReminderRepository reminderRepository;
    private final TicketRepository ticketRepository;
    private final DeliveryService deliveryService;

    public ReminderService(ReminderRepository reminderRepository, TicketRepository ticketRepository,
                           DeliveryService deliveryService) {
        this.reminderRepository = reminderRepository;
        this.ticketRepository = ticketRepository;
        this.deliveryService = deliveryService;
    }

    public List<Reminder> listUnread(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return List.of();
        }
        return reminderRepository.findUnreadByTargetUser(displayName);
    }

    @Transactional
    public void markRead(String id) {
        reminderRepository.markRead(id);
    }

    @Transactional
    public void processDueReminders(int hour) {
        if (!REMIND_HOURS.contains(hour)) {
            return;
        }
        LocalDate today = LocalDate.now();
        String completed = TicketStatus.COMPLETED.getLabel();
        for (int daysBefore : REMIND_DAYS_BEFORE) {
            LocalDate targetDueDate = today.plusDays(daysBefore);
            List<Ticket> tickets = ticketRepository.findActiveByExpectedCompleteDate(completed, targetDueDate);
            for (Ticket ticket : tickets) {
                String message = buildMessage(ticket, daysBefore);
                createReminderIfAbsent(ticket, ticket.getHandler(), message, today, hour);
                createReminderIfAbsent(ticket, ticket.getSubmitter(), message, today, hour);
            }
        }
    }

    @Transactional
    public void catchUpMissedRemindersToday() {
        int currentHour = LocalTime.now().getHour();
        for (int hour : REMIND_HOURS) {
            if (hour <= currentHour) {
                processDueReminders(hour);
            }
        }
    }

    @Transactional
    public void checkAndCreateNow() {
        catchUpMissedRemindersToday();
    }

    private String buildMessage(Ticket ticket, int daysBefore) {
        String projectName = deliveryService.buildDisplayName(ticket.getDelivery());
        String dueDate = ticket.getExpectedCompleteDate().toString();
        String status = ticket.getStatus();
        return switch (daysBefore) {
            case 0 -> "工单「" + projectName + "」预计今日（" + dueDate + "）完成，当前状态「" + status + "」，请及时跟进。";
            case 1 -> "工单「" + projectName + "」预计明日（" + dueDate + "）完成，当前状态「" + status + "」，请提前安排。";
            case 3 -> "工单「" + projectName + "」预计 " + dueDate + " 完成，距今还有 3 天，当前状态「" + status + "」，请关注。";
            default -> "工单「" + projectName + "」请及时跟进。";
        };
    }

    private void createReminderIfAbsent(Ticket ticket, String targetUser, String message,
                                        LocalDate date, int hour) {
        if (targetUser == null || targetUser.isBlank()) {
            return;
        }
        if (reminderRepository.countExisting(ticket.getId(), targetUser, date, hour) > 0) {
            return;
        }
        Reminder reminder = new Reminder();
        reminder.setTicket(ticket);
        reminder.setTargetUser(targetUser);
        reminder.setMessage(message);
        reminder.setRemindDate(date);
        reminder.setRemindHour(hour);
        reminderRepository.save(reminder);
    }
}
