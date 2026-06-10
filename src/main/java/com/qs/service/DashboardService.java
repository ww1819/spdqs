package com.qs.service;

import com.qs.dto.ArchiveView;
import com.qs.dto.DashboardView;
import com.qs.entity.Reminder;
import com.qs.entity.Ticket;
import com.qs.enums.ArchiveStatus;
import com.qs.enums.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final ArchiveService archiveService;
    private final TicketService ticketService;
    private final ReminderService reminderService;

    public DashboardService(ArchiveService archiveService, TicketService ticketService,
                            ReminderService reminderService) {
        this.archiveService = archiveService;
        this.ticketService = ticketService;
        this.reminderService = reminderService;
    }

    public DashboardView build(String currentUser) {
        reminderService.checkAndCreateNow();

        List<ArchiveView> allArchives = archiveService.listAll(null);
        List<ArchiveView> maintAlerts = allArchives.stream()
                .filter(v -> v.getStatus() == ArchiveStatus.EXPIRING_SOON
                        || v.getStatus() == ArchiveStatus.EXPIRED)
                .sorted(Comparator.comparingLong(ArchiveView::getDaysToExpire))
                .toList();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (ArchiveStatus status : ArchiveStatus.values()) {
            long count = allArchives.stream().filter(v -> v.getStatus() == status).count();
            statusCounts.put(status.getLabel(), count);
        }

        List<Ticket> allTickets = ticketService.listAll();
        long activeTickets = allTickets.stream()
                .filter(t -> TicketStatus.isActive(t.getStatus()))
                .count();
        long completedTickets = allTickets.stream()
                .filter(t -> TicketStatus.COMPLETED.getLabel().equals(t.getStatus()))
                .count();
        List<Ticket> recentTickets = ticketService.listForDashboard().stream()
                .limit(20)
                .toList();

        List<Ticket> myTodos = ticketService.findMyTodos(currentUser).stream()
                .sorted(TicketService.dashboardTicketOrder())
                .toList();
        List<Ticket> todayFollowUps = ticketService.findTodayFollowUps();
        List<Reminder> unread = reminderService.listUnread(currentUser);

        return new DashboardView(maintAlerts, myTodos, todayFollowUps, unread,
                allArchives.size(), activeTickets, completedTickets, recentTickets, statusCounts);
    }
}
