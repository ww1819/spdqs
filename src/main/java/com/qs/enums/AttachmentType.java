package com.qs.enums;

public enum AttachmentType {
    IMAGE("图片"),
    FILE("附件"),
    /** 客户签字确认报告（扫描件/照片/PDF） */
    CONFIRM("确认报告");

    private final String label;

    AttachmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
