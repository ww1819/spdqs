package com.qs.enums;

public enum DeliveryStatus {
    LAUNCHING("上线中"),
    MAINTAINING("维保中"),
    EXPIRING_SOON("维保到期在三个月内"),
    EXPIRED("维保到期");

    private final String label;

    DeliveryStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static DeliveryStatus fromLabel(String label) {
        for (DeliveryStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        return null;
    }
}
