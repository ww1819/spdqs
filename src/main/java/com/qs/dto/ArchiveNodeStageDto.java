package com.qs.dto;

public class ArchiveNodeStageDto {

    private String id;
    private String name;
    private int sortOrder;
    private String colorKey;

    public ArchiveNodeStageDto() {
    }

    public ArchiveNodeStageDto(String id, String name, int sortOrder, String colorKey) {
        this.id = id;
        this.name = name;
        this.sortOrder = sortOrder;
        this.colorKey = colorKey;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getColorKey() {
        return colorKey;
    }

    public void setColorKey(String colorKey) {
        this.colorKey = colorKey;
    }
}
