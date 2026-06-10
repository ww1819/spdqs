package com.qs.enums;

public enum ArchiveStatus {
    LAUNCHING("上线中"),
    MAINTAINING("维保中"),
    EXPIRING_SOON("维保到期在三个月内"),
    EXPIRED("维保到期");

    private final String label;

    ArchiveStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ArchiveStatus fromLabel(String label) {
        for (ArchiveStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        return null;
    }
}
