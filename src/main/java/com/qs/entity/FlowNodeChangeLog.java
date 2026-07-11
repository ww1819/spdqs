package com.qs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "T_FLOW_NODE_CHANGE")
public class FlowNodeChangeLog {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "NODE_ID", nullable = false, length = 36)
    private String nodeId;

    @Column(name = "PROJECT_ID", nullable = false, length = 36)
    private String projectId;

    @Column(name = "OLD_TITLE", length = 200)
    private String oldTitle;

    @Column(name = "NEW_TITLE", length = 200)
    private String newTitle;

    @Lob
    @Column(name = "OLD_DESCRIPTION")
    private String oldDescription;

    @Lob
    @Column(name = "NEW_DESCRIPTION")
    private String newDescription;

    @Column(name = "CHANGE_BY", length = 50)
    private String changeBy;

    @Column(name = "CHANGE_TIME", nullable = false)
    private LocalDateTime changeTime;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getOldTitle() {
        return oldTitle;
    }

    public void setOldTitle(String oldTitle) {
        this.oldTitle = oldTitle;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }

    public String getOldDescription() {
        return oldDescription;
    }

    public void setOldDescription(String oldDescription) {
        this.oldDescription = oldDescription;
    }

    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription;
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
