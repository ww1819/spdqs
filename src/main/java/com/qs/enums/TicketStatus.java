package com.qs.enums;

public enum TicketStatus {
    SUBMITTED("已提交"),
    COMMUNICATING("沟通中"),
    PROCESSING("处理中"),
    PENDING_UPGRADE("待升级"),
    COMPLETED("已完成");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TicketStatus fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (TicketStatus status : values()) {
            if (status.label.equals(label)) {
                return status;
            }
        }
        return null;
    }

    public String getBadgeClass() {
        return switch (this) {
            case SUBMITTED -> "bg-secondary";
            case COMMUNICATING -> "bg-info text-dark";
            case PROCESSING -> "bg-warning text-dark";
            case PENDING_UPGRADE -> "bg-danger";
            case COMPLETED -> "bg-success";
        };
    }

    public static boolean isActive(String statusLabel) {
        return statusLabel != null && !COMPLETED.getLabel().equals(statusLabel);
    }
}
