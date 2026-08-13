package com.qs.entity;

import com.qs.enums.TicketProcessAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工单处理进程（开发已处理 / 实施核对回复 / 待反馈调整 / 完成）
 */
@Entity
@Table(name = "T_TICKET_PROCESS")
public class TicketProcess {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TICKET_ID", nullable = false)
    private Ticket ticket;

    /** 挂在某条「已处理」下的回复；根记录为空 */
    @Column(name = "PARENT_ID", length = 36)
    private String parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTION_TYPE", nullable = false, length = 20)
    private TicketProcessAction actionType;

    /** 处理方式（已处理时填写） */
    @Column(name = "HANDLE_METHOD", length = 100)
    private String handleMethod;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Column(name = "CREATE_BY", length = 50)
    private String createBy;

    @Column(name = "CREATE_TIME", nullable = false)
    private LocalDateTime createTime;

    @Transient
    private List<TicketProcess> replies = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createTime == null) {
            createTime = LocalDateTime.now();
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

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public TicketProcessAction getActionType() {
        return actionType;
    }

    public void setActionType(TicketProcessAction actionType) {
        this.actionType = actionType;
    }

    public String getHandleMethod() {
        return handleMethod;
    }

    public void setHandleMethod(String handleMethod) {
        this.handleMethod = handleMethod;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<TicketProcess> getReplies() {
        return replies;
    }

    public void setReplies(List<TicketProcess> replies) {
        this.replies = replies != null ? replies : new ArrayList<>();
    }
}
