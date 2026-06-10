package com.qs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "T_TICKET")
public class Ticket {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ARCHIVE_ID", nullable = false)
    private Archive archive;

    @Column(name = "ORDER_TYPE", nullable = false, length = 30)
    private String orderType;

    @Lob
    @Column(name = "CONTENT")
    private String content;

    @Lob
    @Column(name = "CONTACT_INFO")
    private String contactInfo;

    @Column(name = "SUBMITTER", length = 50)
    private String submitter;

    @Column(name = "HANDLER", length = 50)
    private String handler;

    @Column(name = "STATUS", length = 20)
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "EXPECTED_COMPLETE_DATE")
    private LocalDate expectedCompleteDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "TARGET_COMPLETE_DATE")
    private LocalDate targetCompleteDate;

    @Lob
    @Column(name = "ATTENTION_NOTE")
    private String attentionNote;

    @Lob
    @Column(name = "PROCESS_NOTE")
    private String processNote;

    @Column(name = "CREATE_TIME", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "UPGRADE_BY", length = 50)
    private String upgradeBy;

    @Column(name = "UPGRADE_TIME")
    private LocalDateTime upgradeTime;

    @Column(name = "TICKET_NO")
    private Long ticketNo;

    @Transient
    private boolean hasFollowUpRecord;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "已提交";
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Archive getArchive() {
        return archive;
    }

    public void setArchive(Archive archive) {
        this.archive = archive;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getExpectedCompleteDate() {
        return expectedCompleteDate;
    }

    public void setExpectedCompleteDate(LocalDate expectedCompleteDate) {
        this.expectedCompleteDate = expectedCompleteDate;
    }

    public LocalDate getTargetCompleteDate() {
        return targetCompleteDate;
    }

    public void setTargetCompleteDate(LocalDate targetCompleteDate) {
        this.targetCompleteDate = targetCompleteDate;
    }

    public String getAttentionNote() {
        return attentionNote;
    }

    public void setAttentionNote(String attentionNote) {
        this.attentionNote = attentionNote;
    }

    public String getProcessNote() {
        return processNote;
    }

    public void setProcessNote(String processNote) {
        this.processNote = processNote;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpgradeBy() {
        return upgradeBy;
    }

    public void setUpgradeBy(String upgradeBy) {
        this.upgradeBy = upgradeBy;
    }

    public LocalDateTime getUpgradeTime() {
        return upgradeTime;
    }

    public void setUpgradeTime(LocalDateTime upgradeTime) {
        this.upgradeTime = upgradeTime;
    }

    public Long getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(Long ticketNo) {
        this.ticketNo = ticketNo;
    }

    public boolean hasFollowUpRecord() {
        return hasFollowUpRecord;
    }

    public void setHasFollowUpRecord(boolean hasFollowUpRecord) {
        this.hasFollowUpRecord = hasFollowUpRecord;
    }

    /** @deprecated 使用跟进记录表，保留字段兼容旧数据 */
    public boolean hasProcessNote() {
        return hasFollowUpRecord || (processNote != null && !processNote.isBlank());
    }

    public boolean isTargetDueToday() {
        return targetCompleteDate != null && targetCompleteDate.equals(LocalDate.now());
    }

    /** 目标完成日期为今天或已过期 */
    public boolean isTargetDueOrOverdue() {
        return targetCompleteDate != null && !targetCompleteDate.isAfter(LocalDate.now());
    }

    public boolean hasTargetCompleteDate() {
        return targetCompleteDate != null;
    }
}
