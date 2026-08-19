package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.model.OrderItem;
import com.pharmacy.model.Payment;
import com.pharmacy.pattern.decorator.BaseBillAmount;
import com.pharmacy.pattern.decorator.BillAmountComponent;
import com.pharmacy.pattern.decorator.DiscountDecorator;
import com.pharmacy.pattern.decorator.TaxDecorator;
import com.pharmacy.pattern.observer.InventoryAlertSubject;
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.PaymentRepository;
import com.pharmacy.service.discount.DiscountStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@SuppressWarnings("null")
public class BillingFacade {

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final MedicineRepository medicineRepository;
    private final BillFactory billFactory;
    private final InventoryAlertSubject inventoryAlertSubject;
    private final List<DiscountStrategy> discountStrategies;
    private final BigDecimal taxPercent;

    // DIP: collaborators are injected through the constructor, not created internally.
    public BillingFacade(
            OrderRepository orderRepository,
            BillRepository billRepository,
            PaymentRepository paymentRepository,
            MedicineRepository medicineRepository,
            BillFactory billFactory,
            InventoryAlertSubject inventoryAlertSubject,
            List<DiscountStrategy> discountStrategies,
            @Value("${pharmacy.tax-percent:5}") BigDecimal taxPercent
    ) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.medicineRepository = medicineRepository;
        this.billFactory = billFactory;
        this.inventoryAlertSubject = inventoryAlertSubject;
        this.discountStrategies = discountStrategies;
        this.taxPercent = taxPercent;
    }

    // Facade Pattern: this single method orchestrates stock verification, bill generation and inventory updates.
    @Transactional
    public Bill processCustomerBilling(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Invalid order ID: " + orderId);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot process billing for order with no items");
        }

        if (billRepository.existsByOrderOrderId(orderId)
                || order.getStatus() == Order.OrderStatus.BILLED
                || order.getStatus() == Order.OrderStatus.PAID) {
            throw new IllegalStateException("Order already processed for billing: " + orderId);
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.DECLINED) {
            throw new IllegalStateException("Cancelled or declined orders cannot be billed");
        }

        if (!verifyStockAvailability(order)) {
            throw new IllegalStateException("Insufficient stock for one or more medicines in this order");
        }

        // Strategy selects the discount percent; Decorator applies discount then tax, in that order.
        BigDecimal subtotal = order.calculateSubtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountPercent = resolveDiscount(order.getCustomer());

        BillAmountComponent afterDiscount = new DiscountDecorator(new BaseBillAmount(subtotal), discountPercent);
        BillAmountComponent afterTax = new TaxDecorator(afterDiscount, taxPercent);

        BigDecimal discountedSubtotal = afterDiscount.calculateTotal();
        BigDecimal discountAmount = subtotal.subtract(discountedSubtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = afterTax.calculateTotal();
        BigDecimal taxAmount = total.subtract(discountedSubtotal).setScale(2, RoundingMode.HALF_UP);

        Bill bill = billFactory.createBill(order, subtotal, discountAmount, taxAmount, total);

        for (OrderItem item : order.getItems()) {
            Medicine medicine = item.getMedicine();
            if (medicine == null) {
                throw new IllegalStateException("Order item references a null medicine");
            }
            medicine.reduceStock(item.getQuantity());
            try {
                medicineRepository.saveAndFlush(medicine);
            } catch (OptimisticLockingFailureException e) {
                // Another billing transaction updated this medicine's stock first; fail loud instead of overselling.
                throw new IllegalStateException(
                        "Stock for " + medicine.getName() + " changed while billing; please retry", e);
            }
            inventoryAlertSubject.notifyLowStockOrExpiry(medicine);
        }

        order.calculateTotal();
        order.setStatus(Order.OrderStatus.BILLED);
        orderRepository.save(order);

        Bill savedBill = billRepository.save(bill);
        order.setBill(savedBill);
        return savedBill;
    }

    @Transactional
    public Payment processPayment(Long billId, Payment.PaymentMethod method) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        if (bill.getPayment() != null) {
            return bill.getPayment();
        }

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setAmount(bill.getTotal());
        payment.setPaymentMethod(method == null ? Payment.PaymentMethod.CASH : method);
        payment.processPayment();

        Payment saved = paymentRepository.save(payment);
        bill.setPayment(saved);
        billRepository.save(bill);

        if (saved.getStatus() == Payment.PaymentStatus.SUCCESS) {
            Order order = bill.getOrder();
            order.setStatus(Order.OrderStatus.PAID);
            orderRepository.save(order);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean verifyStockAvailability(Order order) {
        return order.getItems().stream().allMatch(item ->
                item.getMedicine() != null
                        && item.getMedicine().getStockQty() != null
                        && item.getQuantity() != null
                        && item.getMedicine().getStockQty() >= item.getQuantity()
        );
    }

    @Transactional(readOnly = true)
    public List<Bill> customerBillHistory(String customerPhone) {
        return billRepository.findByOrderCustomerPhoneOrderByGeneratedAtDesc(customerPhone);
    }

    private BigDecimal resolveDiscount(Customer customer) {
        return discountStrategies.stream()
                .filter(strategy -> strategy.supports(customer))
                .findFirst()
                .map(strategy -> strategy.discountPercent(customer))
                .orElse(BigDecimal.ZERO);
    }
}
