package com.qs.dto;

public class ArchiveBriefDto {

    private String id;
    private String projectName;
    private String contactInfo;
    private String remoteMethod;
    private String specialProcess;
    private String launchPlan;
    private String onsiteManager;
    private String implManager;

    public ArchiveBriefDto(String id, String projectName, String contactInfo, String remoteMethod,
                           String specialProcess, String launchPlan, String onsiteManager,
                           String implManager) {
        this.id = id;
        this.projectName = projectName;
        this.contactInfo = contactInfo;
        this.remoteMethod = remoteMethod;
        this.specialProcess = specialProcess;
        this.launchPlan = launchPlan;
        this.onsiteManager = onsiteManager;
        this.implManager = implManager;
    }

    public String getId() {
        return id;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public String getRemoteMethod() {
        return remoteMethod;
    }

    public String getSpecialProcess() {
        return specialProcess;
    }

    public String getLaunchPlan() {
        return launchPlan;
    }

    public String getOnsiteManager() {
        return onsiteManager;
    }

    public String getImplManager() {
        return implManager;
    }
}
