package com.qs.enums;

public enum ArchiveNodeStage {
    BUSINESS("商务阶段"),
    RESEARCH("调研阶段"),
    LAUNCH("上线阶段"),
    WARRANTY("质保阶段"),
    MAINTENANCE("维保阶段"),
    OTHER("其他");

    private final String label;

    ArchiveNodeStage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ArchiveNodeStage fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return OTHER;
        }
        for (ArchiveNodeStage stage : values()) {
            if (stage.label.equals(label.trim())) {
                return stage;
            }
        }
        return OTHER;
    }
}
