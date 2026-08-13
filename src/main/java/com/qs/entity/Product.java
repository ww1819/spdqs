package com.qs.entity;

import com.qs.util.IdUtils;
import com.qs.util.PinyinCodeUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "T_PRODUCT")
public class Product {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "CODE", nullable = false, length = 50)
    private String code;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "NAME_PY", length = 100)
    private String namePy;

    @Column(name = "SORT_ORDER", nullable = false)
    private int sortOrder;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Lob
    @Column(name = "REMARK")
    private String remark;

    @Column(name = "CREATE_TIME", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = IdUtils.dashedUuid7();
        }
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        refreshNamePy();
    }

    @PreUpdate
    public void preUpdate() {
        refreshNamePy();
    }

    private void refreshNamePy() {
        namePy = PinyinCodeUtil.toJianpin(name);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePy() {
        return namePy;
    }

    public void setNamePy(String namePy) {
        this.namePy = namePy;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
