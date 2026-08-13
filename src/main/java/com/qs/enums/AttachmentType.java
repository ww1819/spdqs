package com.qs.enums;

public enum AttachmentType {
    IMAGE("图片"),
    FILE("附件"),
    /** 实施完成后的确认报告 */
    CONFIRM("确认报告"),
    /** 实施前的方案确认报告 */
    PLAN_CONFIRM("方案确认");

    private final String label;

    AttachmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
