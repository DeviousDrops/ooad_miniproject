package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_pharmacists")
public class Pharmacist extends User implements PharmacistOperations {

    @Column
    private Long employeeId;

    @Column
    private String licenseNumber;

    @Override
    public String roleName() {
        return "PHARMACIST";
    }

    @Override
    public boolean verifyStock(long medicineId) {
        return medicineId > 0;
    }

    @Override
    public Bill processCustomerBilling(long orderId) {
        return new Bill();
    }

    @Override
    public float applyLoyaltyDiscount(String customerPhone) {
        return customerPhone == null || customerPhone.isBlank() ? 0.0f : 5.0f;
    }

    @Override
    public void updateInventoryStatus(long medicineId, int qty) {
        // Inventory updates are executed in the service layer.
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
