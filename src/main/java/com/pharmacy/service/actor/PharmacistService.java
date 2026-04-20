package com.pharmacy.service.actor;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Inventory;
import com.pharmacy.model.Payment;
import com.pharmacy.service.billing.BillingService;
import com.pharmacy.service.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PharmacistService {

    private final InventoryService inventoryService;
    private final BillingService billingService;

    public PharmacistService(InventoryService inventoryService, BillingService billingService) {
        this.inventoryService = inventoryService;
        this.billingService = billingService;
    }

    public boolean verifyStock(Long medicineId, Integer requiredQty) {
        return inventoryService.verifyStock(medicineId, requiredQty);
    }

    public BillingReceipt processCustomerBilling(
            Long orderId,
            BigDecimal taxPercent,
            BigDecimal discountPercent,
            Payment.PaymentMethod paymentMethod
    ) {
        Bill bill = billingService.generateBill(orderId, taxPercent, discountPercent);
        Payment payment = billingService.processPayment(bill.getBillId(), paymentMethod);
        return new BillingReceipt(bill, payment);
    }

    public Inventory updateInventoryStatus(Long inventoryId) {
        return inventoryService.updateInventoryStatus(inventoryId);
    }

    public record BillingReceipt(Bill bill, Payment payment) {
    }
}
