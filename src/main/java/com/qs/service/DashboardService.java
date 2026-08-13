package com.qs.service;

import com.qs.dto.DeliveryView;
import com.qs.dto.DashboardView;
import com.qs.entity.Reminder;
import com.qs.entity.Ticket;
import com.qs.enums.DeliveryStatus;
import com.qs.enums.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardService {

    private final DeliveryService deliveryService;
    private final TicketService ticketService;
    private final ReminderService reminderService;

    public DashboardService(DeliveryService deliveryService, TicketService ticketService,
                            ReminderService reminderService) {
        this.deliveryService = deliveryService;
        this.ticketService = ticketService;
        this.reminderService = reminderService;
    }

    public DashboardView build(String currentUser) {
        return build(currentUser, null);
    }

    public DashboardView build(String currentUser, Set<String> allowedDeliveryIds) {
        reminderService.checkAndCreateNow();

        List<DeliveryView> allDeliveries = deliveryService.listAll(null, null, allowedDeliveryIds);
        List<DeliveryView> maintAlerts = allDeliveries.stream()
                .filter(v -> v.getStatus() == DeliveryStatus.EXPIRING_SOON
                        || v.getStatus() == DeliveryStatus.EXPIRED)
                .sorted(Comparator.comparingLong(DeliveryView::getDaysToExpire))
                .toList();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (DeliveryStatus status : DeliveryStatus.values()) {
            long count = allDeliveries.stream().filter(v -> v.getStatus() == status).count();
            statusCounts.put(status.getLabel(), count);
        }

        List<Ticket> scopedTickets = ticketService.listAll().stream()
                .filter(t -> allowedDeliveryIds == null
                        || (t.getDelivery() != null && allowedDeliveryIds.contains(t.getDelivery().getId())))
                .toList();
        long activeTickets = scopedTickets.stream()
                .filter(t -> TicketStatus.isActive(t.getStatus()))
                .count();
        long completedTickets = scopedTickets.stream()
                .filter(t -> TicketStatus.COMPLETED.getLabel().equals(t.getStatus()))
                .count();
        List<Ticket> recentTickets = ticketService.listForDashboard().stream()
                .filter(t -> allowedDeliveryIds == null
                        || (t.getDelivery() != null && allowedDeliveryIds.contains(t.getDelivery().getId())))
                .limit(20)
                .toList();

        List<Ticket> myTodos = ticketService.findMyTodos(currentUser).stream()
                .filter(t -> allowedDeliveryIds == null
                        || (t.getDelivery() != null && allowedDeliveryIds.contains(t.getDelivery().getId())))
                .sorted(TicketService.dashboardTicketOrder())
                .toList();
        List<Ticket> todayFollowUps = ticketService.findTodayFollowUps().stream()
                .filter(t -> allowedDeliveryIds == null
                        || (t.getDelivery() != null && allowedDeliveryIds.contains(t.getDelivery().getId())))
                .toList();
        List<Reminder> unread = reminderService.listUnread(currentUser);

        return new DashboardView(maintAlerts, myTodos, todayFollowUps, unread,
                allDeliveries.size(), activeTickets, completedTickets, recentTickets, statusCounts);
    }
}
