package com.qs.enums;

public enum DeliveryNodeType {
    POINT("时间点"),
    RANGE("时间段");

    private final String label;

    DeliveryNodeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static DeliveryNodeType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return POINT;
        }
        for (DeliveryNodeType type : values()) {
            if (type.label.equals(label.trim()) || type.name().equalsIgnoreCase(label.trim())) {
                return type;
            }
        }
        return POINT;
    }
}
