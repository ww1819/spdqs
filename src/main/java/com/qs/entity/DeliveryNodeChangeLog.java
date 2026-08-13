package com.qs.entity;

import com.qs.util.IdUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "T_DELIVERY_NODE_CHANGE")
public class DeliveryNodeChangeLog {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NODE_ID", nullable = false, length = 36)
    private String nodeId;

    @Column(name = "DELIVERY_ID", nullable = false, length = 36)
    private String deliveryId;

    @Column(name = "FIELD_NAME", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "FIELD_LABEL", length = 50)
    private String fieldLabel;

    @Lob
    @Column(name = "OLD_VALUE")
    private String oldValue;

    @Lob
    @Column(name = "NEW_VALUE")
    private String newValue;

    @Column(name = "CHANGE_BY", length = 50)
    private String changeBy;

    @Column(name = "CHANGE_TIME", nullable = false)
    private LocalDateTime changeTime;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = IdUtils.dashedUuid7();
        }
        if (changeTime == null) {
            changeTime = LocalDateTime.now();
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

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangeBy() {
        return changeBy;
    }

    public void setChangeBy(String changeBy) {
        this.changeBy = changeBy;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
    }
}
