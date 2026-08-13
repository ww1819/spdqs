package com.qs.dto;

public class DeliveryBriefDto {

    private final String id;
    private final String displayName;
    private final String contactInfo;
    private final String remoteMethod;
    private final String specialProcess;
    private final String launchPlan;
    private final String onsiteManager;
    private final String implManager;

    public DeliveryBriefDto(String id, String displayName, String contactInfo, String remoteMethod,
                            String specialProcess, String launchPlan, String onsiteManager, String implManager) {
        this.id = id;
        this.displayName = displayName;
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

    public String getDisplayName() {
        return displayName;
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
