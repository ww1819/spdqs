package com.qs.enums;

public enum OrderType {
    OPTIMIZE("优化"),
    REQUIREMENT("需求"),
    API("接口"),
    LAUNCH("上线"),
    PAYMENT("付款"),
    PATIENT_BILLING("病人计费"),
    CRITICAL_FAULT("系统严重故障"),
    DAILY("日常工单"),
    SUGGESTION("优化建议");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static OrderType fromLabel(String label) {
        if (label == null) {
            return null;
        }
        for (OrderType type : values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        return null;
    }
}
