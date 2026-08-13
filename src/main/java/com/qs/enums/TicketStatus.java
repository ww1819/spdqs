package com.qs.enums;

public enum TicketStatus {
    SUBMITTED("已提交"),
    COMMUNICATING("沟通中"),
    PROCESSING("处理中"),
    HANDLED("已处理"),
    /** 实施核对后需开发按反馈继续调整（温和表述，避免「仍需完善」对抗感） */
    NEED_FEEDBACK("待反馈调整"),
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
            case HANDLED -> "bg-primary";
            case NEED_FEEDBACK -> "bg-warning text-dark";
            case PENDING_UPGRADE -> "bg-danger";
            case COMPLETED -> "bg-success";
        };
    }

    public static boolean isActive(String statusLabel) {
        return statusLabel != null && !COMPLETED.getLabel().equals(statusLabel);
    }

    /** 开发可点击「已处理」的状态 */
    public static boolean canMarkHandled(String statusLabel) {
        return isActive(statusLabel) && !HANDLED.getLabel().equals(statusLabel);
    }

    /** 实施可在「已处理」下确认完成或退回反馈 */
    public static boolean canConfirmHandled(String statusLabel) {
        return HANDLED.getLabel().equals(statusLabel);
    }
}
