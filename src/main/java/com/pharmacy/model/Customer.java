package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_customers")
public class Customer extends User {

    @Column
    private Long customerId;

    @Column(nullable = false)
    private Integer loyaltyPoints = 0;

    @Column
    private String address;

    @PrePersist
    void ensureDefaults() {
        if (loyaltyPoints == null) {
            loyaltyPoints = 0;
        }
    }

    @Override
    public String roleName() {
        return "CUSTOMER";
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
