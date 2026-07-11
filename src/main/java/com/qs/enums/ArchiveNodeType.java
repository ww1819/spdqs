package com.qs.enums;

public enum ArchiveNodeType {
    POINT("时间点"),
    RANGE("时间段");

    private final String label;

    ArchiveNodeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ArchiveNodeType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return POINT;
        }
        for (ArchiveNodeType type : values()) {
            if (type.label.equals(label.trim()) || type.name().equalsIgnoreCase(label.trim())) {
                return type;
            }
        }
        return POINT;
    }
}
