package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_admins")
public class Admin extends User {

    @Column
    private Integer adminLevel;

    @Override
    public String roleName() {
        return "ADMIN";
    }

    public Integer getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(Integer adminLevel) {
        this.adminLevel = adminLevel;
    }
}
