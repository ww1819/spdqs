package com.qs.enums;

import java.util.Arrays;
import java.util.List;

public enum MenuCode {
    DASHBOARD("dashboard", "工作台", "/dashboard"),
    ARCHIVES("archives", "档案列表", "/archives"),
    TICKETS("tickets", "工单列表", "/tickets"),
    ANALYSIS("analysis", "项目分析", "/analysis"),
    USERS("users", "账号管理", "/users");

    private final String code;
    private final String label;
    private final String pathPrefix;

    MenuCode(String code, String label, String pathPrefix) {
        this.code = code;
        this.label = label;
        this.pathPrefix = pathPrefix;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public static MenuCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MenuCode menu : values()) {
            if (menu.code.equals(code)) {
                return menu;
            }
        }
        return null;
    }

    /** 新注册用户默认菜单（不含账号管理、项目分析） */
    public static List<MenuCode> defaultMenusForNewUser() {
        return List.of(DASHBOARD, ARCHIVES, TICKETS);
    }

    public static List<MenuCode> allMenus() {
        return Arrays.asList(values());
    }
}
