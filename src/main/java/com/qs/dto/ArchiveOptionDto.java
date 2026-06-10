package com.qs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qs.enums.ArchiveStatus;

import java.time.LocalDate;

public class ArchiveOptionDto {

    private String id;
    private String projectName;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maintExpireDate;
    private String status;
    private long daysToExpire;

    public ArchiveOptionDto(String id, String projectName, LocalDate maintExpireDate,
                            ArchiveStatus status, long daysToExpire) {
        this.id = id;
        this.projectName = projectName;
        this.maintExpireDate = maintExpireDate;
        this.status = status.getLabel();
        this.daysToExpire = daysToExpire;
    }

    public String getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public LocalDate getMaintExpireDate() {
        return maintExpireDate;
    }

    public String getStatus() {
        return status;
    }

    public long getDaysToExpire() {
        return daysToExpire;
    }
}
