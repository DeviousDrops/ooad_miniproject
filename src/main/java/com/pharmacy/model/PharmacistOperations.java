package com.pharmacy.model;

// ISP: pharmacist-specific contract kept narrow and role-focused.
public interface PharmacistOperations {

    boolean verifyStock(long medicineId);

    Bill processCustomerBilling(long orderId);

    float applyLoyaltyDiscount(long customerId);

    void updateInventoryStatus(long medicineId, int qty);
}
