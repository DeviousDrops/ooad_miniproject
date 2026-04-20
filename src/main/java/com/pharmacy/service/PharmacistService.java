package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("portalPharmacistService")
public class PharmacistService {

    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final BillingFacade billingFacade;
    private final InventoryObserver inventoryObserver;

    public PharmacistService(
            MedicineRepository medicineRepository,
            CustomerRepository customerRepository,
            BillingFacade billingFacade,
            InventoryObserver inventoryObserver
    ) {
        this.medicineRepository = medicineRepository;
        this.customerRepository = customerRepository;
        this.billingFacade = billingFacade;
        this.inventoryObserver = inventoryObserver;
    }

    @Transactional(readOnly = true)
    public boolean verifyStock(long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));
        return medicine.getStockQty() != null && medicine.getStockQty() > 0;
    }

    @Transactional
    public Bill processCustomerBilling(long orderId) {
        return billingFacade.processCustomerBilling(orderId);
    }

    @Transactional(readOnly = true)
    public float applyLoyaltyDiscount(long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        int loyalty = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();
        if (loyalty >= 200) {
            return 15.0f;
        }
        if (loyalty >= 100) {
            return 10.0f;
        }
        if (loyalty >= 1) {
            return 5.0f;
        }
        return 0.0f;
    }

    @Transactional
    public void updateInventoryStatus(long medicineId, int qty) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));

        if (qty >= 0) {
            medicine.increaseStock(qty);
        } else {
            medicine.reduceStock(Math.abs(qty));
        }
        medicineRepository.save(medicine);
        inventoryObserver.checkLowStock(medicine);
    }
}
