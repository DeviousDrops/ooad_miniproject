package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("portalPharmacistService")
public class PharmacistService {

    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final BillingFacade billingFacade;
    private final InventoryObserver inventoryObserver;

    public PharmacistService(
            MedicineRepository medicineRepository,
            CustomerRepository customerRepository,
            BillRepository billRepository,
            OrderRepository orderRepository,
            BillingFacade billingFacade,
            InventoryObserver inventoryObserver
    ) {
        this.medicineRepository = medicineRepository;
        this.customerRepository = customerRepository;
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
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

    @Transactional
    public Order declineOrder(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != Order.OrderStatus.CREATED) {
            throw new IllegalStateException("Only pending orders can be declined");
        }

        order.setStatus(Order.OrderStatus.DECLINED);
        return orderRepository.saveAndFlush(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findUnprocessedOrders() {
        return orderRepository.findByStatusOrderByOrderedAtAsc(Order.OrderStatus.CREATED);
    }

    @Transactional(readOnly = true)
    public List<Bill> findProcessedBills() {
        return billRepository.findAllByOrderByGeneratedAtDesc();
    }

    @Transactional(readOnly = true)
    public float applyLoyaltyDiscount(String customerPhone) {
        Customer customer = customerRepository.findByPhone(customerPhone)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerPhone));

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
