package com.qs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "T_REMINDER")
public class Reminder {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TICKET_ID")
    private Ticket ticket;

    @Column(name = "TARGET_USER", nullable = false, length = 50)
    private String targetUser;

    @Column(name = "MESSAGE", nullable = false, length = 500)
    private String message;

    @Column(name = "REMIND_DATE", nullable = false)
    private LocalDate remindDate;

    @Column(name = "REMIND_HOUR", nullable = false)
    private int remindHour;

    @Column(name = "IS_READ", nullable = false, length = 1)
    private String isRead;

    @Column(name = "CREATE_TIME", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = "0";
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getTargetUser() {
        return targetUser;
    }

    public void setTargetUser(String targetUser) {
        this.targetUser = targetUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getRemindDate() {
        return remindDate;
    }

    public void setRemindDate(LocalDate remindDate) {
        this.remindDate = remindDate;
    }

    public int getRemindHour() {
        return remindHour;
    }

    public void setRemindHour(int remindHour) {
        this.remindHour = remindHour;
    }

    public String getIsRead() {
        return isRead;
    }

    public void setIsRead(String isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean isUnread() {
        return !"1".equals(isRead);
    }
}
