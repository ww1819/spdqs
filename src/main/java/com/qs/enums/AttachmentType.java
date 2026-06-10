package com.qs.enums;

public enum AttachmentType {
    IMAGE("图片"),
    FILE("附件");

    private final String label;

    AttachmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
