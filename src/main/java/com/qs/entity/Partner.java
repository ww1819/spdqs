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
@Table(name = "T_PARTNER")
public class Partner {

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "CODE", length = 50)
    private String code;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "NAME_PY", length = 100)
    private String namePy;

    @Lob
    @Column(name = "CONTACT")
    private String contact;

    @Lob
    @Column(name = "REMARK")
    private String remark;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "CREATE_BY", length = 50)
    private String createBy;

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
        if (status == null || status.isBlank()) {
            status = "启用";
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

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
