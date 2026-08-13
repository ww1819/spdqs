package com.qs.enums;

public enum TicketProcessAction {
    HANDLED("已处理"),
    REPLY("核对回复"),
    NEED_FEEDBACK("待反馈调整"),
    COMPLETE("已完成");

    private final String label;

    TicketProcessAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
