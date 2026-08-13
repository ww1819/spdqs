package com.qs.dto;

public class ArchiveNodeStageRequest {

    private String name;
    /** 支持小数：如 1.55 表示插入到 1 与 2 之间，服务端进位并为后续序号腾位 */
    private Double sortOrder;
    private String colorKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Double sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getColorKey() {
        return colorKey;
    }

    public void setColorKey(String colorKey) {
        this.colorKey = colorKey;
    }
}
