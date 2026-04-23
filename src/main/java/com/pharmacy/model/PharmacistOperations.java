package com.pharmacy.model;

// ISP: pharmacist-specific contract kept narrow and role-focused.
public interface PharmacistOperations {

    boolean verifyStock(long medicineId);

    Bill processCustomerBilling(long orderId);

    float applyLoyaltyDiscount(String customerPhone);

    void updateInventoryStatus(long medicineId, int qty);
}
