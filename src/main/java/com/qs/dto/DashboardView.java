package com.qs.dto;

import com.qs.entity.Reminder;
import com.qs.entity.Ticket;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardView {

    private final List<ArchiveView> maintAlerts;
    private final List<Ticket> myTodos;
    private final List<Ticket> todayFollowUps;
    private final List<Reminder> unreadReminders;
    private final int totalArchives;
    private final long activeTickets;
    private final long completedTickets;
    private final List<Ticket> recentTickets;
    private final Map<String, Long> archiveStatusCounts;

    public DashboardView(List<ArchiveView> maintAlerts, List<Ticket> myTodos,
                         List<Ticket> todayFollowUps, List<Reminder> unreadReminders,
                         int totalArchives, long activeTickets, long completedTickets,
                         List<Ticket> recentTickets, Map<String, Long> archiveStatusCounts) {
        this.maintAlerts = maintAlerts;
        this.myTodos = myTodos;
        this.todayFollowUps = todayFollowUps;
        this.unreadReminders = unreadReminders;
        this.totalArchives = totalArchives;
        this.activeTickets = activeTickets;
        this.completedTickets = completedTickets;
        this.recentTickets = recentTickets;
        this.archiveStatusCounts = archiveStatusCounts;
    }

    public List<ArchiveView> getMaintAlerts() {
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

    public int getTotalArchives() {
        return totalArchives;
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

    public Map<String, Long> getArchiveStatusCounts() {
        return archiveStatusCounts;
    }

    public int getPendingCount() {
        return todayFollowUps.size() + unreadReminders.size();
    }
}
