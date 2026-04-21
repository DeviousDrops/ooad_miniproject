package com.pharmacy.service.actor;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.model.Prescription;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.PrescriptionRepository;
import com.pharmacy.service.billing.BillingService;
import com.pharmacy.service.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final InventoryService inventoryService;
    private final BillingService billingService;
    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;

    public CustomerService(
            InventoryService inventoryService,
            BillingService billingService,
            OrderRepository orderRepository,
            PrescriptionRepository prescriptionRepository
    ) {
        this.inventoryService = inventoryService;
        this.billingService = billingService;
        this.orderRepository = orderRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String keyword) {
        return inventoryService.searchMedicines(keyword);
    }

    public Order placeOrder(String customerPhone, List<BillingService.OrderLineCommand> items) {
        return billingService.placeOrder(customerPhone, items);
    }

    @Transactional(readOnly = true)
    public List<Order> viewOrderHistory(String customerPhone) {
        return orderRepository.findByCustomerPhoneOrderByOrderedAtDesc(customerPhone);
    }

    @Transactional(readOnly = true)
    public List<Prescription> viewPrescriptionHistory(String customerPhone) {
        return prescriptionRepository.findByCustomerPhone(customerPhone);
    }

    @Transactional(readOnly = true)
    public List<Bill> viewBillHistory(String customerPhone) {
        return billingService.customerBillHistory(customerPhone);
    }
}
