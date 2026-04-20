package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_admins")
public class Admin extends User implements AdminOperations {

    @Column
    private Integer adminLevel;

    @Override
    public String roleName() {
        return "ADMIN";
    }

    @Override
    public void manageMedicineInventory() {
        // Inventory governance is orchestrated in AdminService.
    }

    @Override
    public Report generateSalesAnalytics() {
        return new Report();
    }

    @Override
    public List<String> fetchSalesData() {
        return new ArrayList<>();
    }

    @Override
    public void automateMedicineSupply() {
        // Scheduling and automation are handled by application services.
    }

    public Integer getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(Integer adminLevel) {
        this.adminLevel = adminLevel;
    }
}
