package com.pharmacy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_customers")
public class Customer extends User implements CustomerOperations {

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

    @Override
    public List<Medicine> searchMedicines(String name) {
        return new ArrayList<>();
    }

    @Override
    public Order placeOrder(Order order) {
        return order;
    }

    @Override
    public List<Prescription> viewPrescriptionHistory() {
        return new ArrayList<>();
    }

    @Override
    public boolean makePayment(Payment payment) {
        return payment != null && payment.processPayment() && payment.validateTransaction();
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
