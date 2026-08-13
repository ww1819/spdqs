package com.qs.dto;

import com.qs.entity.Reminder;
import com.qs.entity.Ticket;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardView {

    private final List<DeliveryView> maintAlerts;
    private final List<Ticket> myTodos;
    private final List<Ticket> todayFollowUps;
    private final List<Reminder> unreadReminders;
    private final int totalDeliveries;
    private final long activeTickets;
    private final long completedTickets;
    private final List<Ticket> recentTickets;
    private final Map<String, Long> deliveryStatusCounts;

    public DashboardView(List<DeliveryView> maintAlerts, List<Ticket> myTodos,
                         List<Ticket> todayFollowUps, List<Reminder> unreadReminders,
                         int totalDeliveries, long activeTickets, long completedTickets,
                         List<Ticket> recentTickets, Map<String, Long> deliveryStatusCounts) {
        this.maintAlerts = maintAlerts;
        this.myTodos = myTodos;
        this.todayFollowUps = todayFollowUps;
        this.unreadReminders = unreadReminders;
        this.totalDeliveries = totalDeliveries;
        this.activeTickets = activeTickets;
        this.completedTickets = completedTickets;
        this.recentTickets = recentTickets;
        this.deliveryStatusCounts = deliveryStatusCounts;
    }

    public List<DeliveryView> getMaintAlerts() {
        return maintAlerts;
    }

    public List<Ticket> getMyTodos() {
        return myTodos;
    }

    public List<Ticket> getTodayFollowUps() {
        return todayFollowUps;
    }

    public List<Reminder> getUnreadReminders() {
        return unreadReminders;
    }

    public int getUnreadReminderCount() {
        return unreadReminders.size();
    }

    public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public long getActiveTickets() {
        return activeTickets;
    }

    public long getCompletedTickets() {
        return completedTickets;
    }

    public List<Ticket> getRecentTickets() {
        return recentTickets;
    }

    public Map<String, Long> getDeliveryStatusCounts() {
        return deliveryStatusCounts;
    }

    public int getPendingCount() {
        return todayFollowUps.size() + unreadReminders.size();
    }
}
