package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_suppliers")
public class Supplier extends User implements SupplierOperations {

    @Column
    private Long supplierId;

    @Column
    private String companyName;

    @Column
    private String contactInfo;

    @Override
    public String roleName() {
        return "SUPPLIER";
    }

    @Override
    public void supplyRestock(Medicine medicine, int qty) {
        if (medicine != null && qty > 0) {
            medicine.increaseStock(qty);
        }
    }

    @Override
    public boolean shipmentVerification(long shipmentId) {
        return shipmentId > 0;
    }

    @Override
    public Invoice submitDigitalInvoice() {
        return new Invoice();
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
}
