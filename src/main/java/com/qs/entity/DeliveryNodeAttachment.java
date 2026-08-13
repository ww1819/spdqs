package com.qs.entity;

import com.qs.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "T_DELIVERY_NODE_ATTACHMENT")
public class DeliveryNodeAttachment {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NODE_ID", nullable = false, length = 36)
    private String nodeId;

    @Column(name = "DELIVERY_ID", nullable = false, length = 36)
    private String deliveryId;

    @Column(name = "ORIGINAL_NAME", nullable = false, length = 255)
    private String originalName;

    @Column(name = "STORED_NAME", nullable = false, length = 255)
    private String storedName;

    @Column(name = "RELATIVE_PATH", nullable = false, length = 500)
    private String relativePath;

    @Column(name = "CONTENT_TYPE", length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    @Column(name = "CREATE_BY", length = 50)
    private String createBy;

    @Column(name = "CREATE_TIME", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "CONFIRMED", nullable = false)
    private boolean confirmed;

    @Column(name = "CONFIRMED_BY", length = 50)
    private String confirmedBy;

    @Column(name = "CONFIRMED_TIME")
    private LocalDateTime confirmedTime;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = IdUtils.dashedUuid7();
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

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(String confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public LocalDateTime getConfirmedTime() {
        return confirmedTime;
    }

    public void setConfirmedTime(LocalDateTime confirmedTime) {
        this.confirmedTime = confirmedTime;
    }
}
