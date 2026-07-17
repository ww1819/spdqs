package com.qs.dto;

import java.util.ArrayList;
import java.util.List;

public class FlowNodeTreeDto {

    private String id;
    private String title;
    private String pinyinCode;
    private String description;
    private boolean hasDescription;
    private List<FlowNodeTreeDto> children = new ArrayList<>();

    public FlowNodeTreeDto() {
    }

    public FlowNodeTreeDto(String id, String title, String description) {
        this(id, title, null, description);
    }

    public FlowNodeTreeDto(String id, String title, String pinyinCode, String description) {
        this.id = id;
        this.title = title;
        this.pinyinCode = pinyinCode;
        this.description = description;
        this.hasDescription = description != null && !description.isBlank();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPinyinCode() {
        return pinyinCode;
    }

    public void setPinyinCode(String pinyinCode) {
        this.pinyinCode = pinyinCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.hasDescription = description != null && !description.isBlank();
    }

    public boolean isHasDescription() {
        return hasDescription;
    }

    public void setHasDescription(boolean hasDescription) {
        this.hasDescription = hasDescription;
    }

    public List<FlowNodeTreeDto> getChildren() {
        return children;
    }

    public void setChildren(List<FlowNodeTreeDto> children) {
        this.children = children;
    }
}
