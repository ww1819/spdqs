package com.qs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "T_ARCHIVE")
public class Archive {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "PROJECT_NAME", nullable = false, length = 200)
    private String projectName;

    @Column(name = "PROJECT_TYPE", length = 100)
    private String projectType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "LAUNCH_DATE")
    private LocalDate launchDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "MAINT_EXPIRE_DATE")
    private LocalDate maintExpireDate;

    @Lob
    @Column(name = "LAUNCH_PLAN")
    private String launchPlan;

    @Lob
    @Column(name = "SPECIAL_PROCESS")
    private String specialProcess;

    @Lob
    @Column(name = "CONTACT_INFO")
    private String contactInfo;

    @Lob
    @Column(name = "REMOTE_METHOD")
    private String remoteMethod;

    @Column(name = "ONSITE_MANAGER", length = 100)
    private String onsiteManager;

    @Column(name = "IMPL_MANAGER", length = 100)
    private String implManager;

    @Column(name = "CREATE_BY", length = 50)
    private String createBy;

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
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public LocalDate getLaunchDate() {
        return launchDate;
    }

    public void setLaunchDate(LocalDate launchDate) {
        this.launchDate = launchDate;
    }

    public LocalDate getMaintExpireDate() {
        return maintExpireDate;
    }

    public void setMaintExpireDate(LocalDate maintExpireDate) {
        this.maintExpireDate = maintExpireDate;
    }

    public String getLaunchPlan() {
        return launchPlan;
    }

    public void setLaunchPlan(String launchPlan) {
        this.launchPlan = launchPlan;
    }

    public String getSpecialProcess() {
        return specialProcess;
    }

    public void setSpecialProcess(String specialProcess) {
        this.specialProcess = specialProcess;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getRemoteMethod() {
        return remoteMethod;
    }

    public void setRemoteMethod(String remoteMethod) {
        this.remoteMethod = remoteMethod;
    }

    public String getOnsiteManager() {
        return onsiteManager;
    }

    public void setOnsiteManager(String onsiteManager) {
        this.onsiteManager = onsiteManager;
    }

    public String getImplManager() {
        return implManager;
    }

    public void setImplManager(String implManager) {
        this.implManager = implManager;
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
}
