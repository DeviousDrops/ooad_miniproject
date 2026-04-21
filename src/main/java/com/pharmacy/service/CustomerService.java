package com.pharmacy.service;

import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.model.OrderItem;
import com.pharmacy.model.Payment;
import com.pharmacy.model.Prescription;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service("portalCustomerService")
@SuppressWarnings("null")
public class CustomerService {

    private final MedicineRepository medicineRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final BillingFacade billingFacade;

    // SRP: customer-specific use cases are centralized in a dedicated service.
    public CustomerService(
            MedicineRepository medicineRepository,
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            PrescriptionRepository prescriptionRepository,
            BillingFacade billingFacade
    ) {
        this.medicineRepository = medicineRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.billingFacade = billingFacade;
    }

    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return medicineRepository.findAll();
        }
        return medicineRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword);
    }

    @Transactional
    public Order placeOrder(Long customerId, List<OrderRequestItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order requires at least one medicine");
        }

        Customer customer = resolveCustomer(customerId);

        Order order = new Order();
        order.setCustomer(customer);

        for (OrderRequestItem line : items) {
            Medicine medicine = medicineRepository.findById(line.medicineId())
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + line.medicineId()));

            if (line.quantity() == null || line.quantity() < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }

            if (medicine.getStockQty() < line.quantity()) {
                throw new IllegalStateException("Insufficient stock for medicine: " + medicine.getName());
            }

            OrderItem item = new OrderItem();
            item.setMedicine(medicine);
            item.setQuantity(line.quantity());
            item.setUnitPrice(medicine.getPrice());
            item.setLineTotal(medicine.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            order.addItem(item);
        }

        order.calculateTotal();
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Prescription> viewPrescriptionHistory(Long customerId) {
        Customer customer = resolveCustomer(customerId);
        return prescriptionRepository.findByCustomerUserId(customer.getUserId());
    }

    @Transactional
    public Payment makePayment(Long billId, Payment.PaymentMethod method) {
        return billingFacade.processPayment(billId, method);
    }

    @Transactional(readOnly = true)
    public List<com.pharmacy.model.Bill> viewBillHistory(Long customerId) {
        Customer customer = resolveCustomer(customerId);
        return billingFacade.customerBillHistory(customer.getUserId());
    }

    @Transactional(readOnly = true)
    public Long defaultCustomerReference() {
        return customerRepository.findAll().stream()
                .findFirst()
                .map(customer -> customer.getCustomerId() != null ? customer.getCustomerId() : customer.getUserId())
                .orElse(1L);
    }

    private Customer resolveCustomer(Long customerRef) {
        if (customerRef == null) {
            throw new IllegalArgumentException("Customer id is required");
        }

        return customerRepository.findById(customerRef)
                .or(() -> customerRepository.findByCustomerId(customerRef))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerRef));
    }

    public record OrderRequestItem(Long medicineId, Integer quantity) {
    }
}
