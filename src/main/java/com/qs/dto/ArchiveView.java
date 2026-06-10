package com.qs.dto;

import com.qs.entity.Archive;
import com.qs.enums.ArchiveStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ArchiveView {

    private final Archive archive;
    private final ArchiveStatus status;
    private final long daysToExpire;

    public ArchiveView(Archive archive, ArchiveStatus status, long daysToExpire) {
        this.archive = archive;
        this.status = status;
        this.daysToExpire = daysToExpire;
    }

    public String getId() {
        return archive.getId();
    }

    public String getProjectName() {
        return archive.getProjectName();
    }

    public String getProjectType() {
        return archive.getProjectType();
    }

    public LocalDate getLaunchDate() {
        return archive.getLaunchDate();
    }

    public LocalDate getMaintExpireDate() {
        return archive.getMaintExpireDate();
    }

    public String getLaunchPlan() {
        return archive.getLaunchPlan();
    }

    public String getSpecialProcess() {
        return archive.getSpecialProcess();
    }

    public String getContactInfo() {
        return archive.getContactInfo();
    }

    public String getRemoteMethod() {
        return archive.getRemoteMethod();
    }

    public String getOnsiteManager() {
        return archive.getOnsiteManager();
    }

    public String getImplManager() {
        return archive.getImplManager();
    }

    public String getCreateBy() {
        return archive.getCreateBy();
    }

    public LocalDateTime getCreateTime() {
        return archive.getCreateTime();
    }

    public Archive getArchive() {
        return archive;
    }

    public ArchiveStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return status.getLabel();
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case LAUNCHING -> "bg-primary";
            case MAINTAINING -> "bg-success";
            case EXPIRING_SOON -> "bg-warning text-dark";
            case EXPIRED -> "bg-danger";
        };
    }

    public long getDaysToExpire() {
        return daysToExpire;
    }
}
