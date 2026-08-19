package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_pharmacists")
public class Pharmacist extends User {

    @Column
    private Long employeeId;

    @Column
    private String licenseNumber;

    @Override
    public String roleName() {
        return "PHARMACIST";
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
}
