package com.pharmacy.service.billing;

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
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.PaymentRepository;
import com.pharmacy.service.inventory.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class BillingService {

    private final OrderRepository orderRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final InventoryService inventoryService;

    public BillingService(
            OrderRepository orderRepository,
            BillRepository billRepository,
            PaymentRepository paymentRepository,
            CustomerRepository customerRepository,
            InventoryService inventoryService
    ) {
        this.orderRepository = orderRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.inventoryService = inventoryService;
    }

    public Order placeOrder(String customerPhone, List<OrderLineCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order requires at least one order item");
        }

        String safeCustomerPhone = Objects.requireNonNull(customerPhone, "customerPhone is required");
        Customer customer = customerRepository.findByPhone(safeCustomerPhone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        Order order = new Order();
        order.setCustomer(customer);

        Map<Long, Integer> quantityByMedicine = new LinkedHashMap<>();
        for (OrderLineCommand line : items) {
            Medicine medicine = inventoryService.getMedicine(line.medicineId());
            quantityByMedicine.put(line.medicineId(), line.quantity());

            OrderItem item = new OrderItem();
            item.setMedicine(medicine);
            item.setQuantity(line.quantity());
            item.setUnitPrice(medicine.getPrice());
            item.setLineTotal(medicine.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
            order.addItem(item);
        }

        Map<Long, Boolean> verification = inventoryService.verifyBulkStock(quantityByMedicine);
        boolean allAvailable = verification.values().stream().allMatch(Boolean::booleanValue);
        if (!allAvailable) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock verification failed for one or more medicines");
        }

        return orderRepository.save(order);
    }

    public Bill generateBill(Long orderId, BigDecimal taxPercent, BigDecimal discountPercent) {
        Long safeOrderId = Objects.requireNonNull(orderId, "orderId is required");
        Order order = orderRepository.findById(safeOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getBill() != null) {
            return order.getBill();
        }

        BigDecimal subtotal = order.calculateSubtotal().setScale(2, RoundingMode.HALF_UP);
        BillAmountComponent taxedComponent = new TaxDecorator(new BaseBillAmount(subtotal), sanitizeRate(taxPercent));
        BigDecimal amountAfterTax = taxedComponent.calculateTotal();

        BillAmountComponent discountedComponent = new DiscountDecorator(new BaseBillAmount(amountAfterTax), sanitizeRate(discountPercent));
        BigDecimal total = discountedComponent.calculateTotal();

        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setSubtotal(subtotal);
        bill.setTaxAmount(amountAfterTax.subtract(subtotal));
        bill.setDiscountAmount(amountAfterTax.subtract(total));
        bill.setTotal(total);

        Map<Long, Integer> quantityByMedicine = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            quantityByMedicine.put(item.getMedicine().getMedicineId(), item.getQuantity());
        }

        inventoryService.deductStockForBilling(quantityByMedicine);

        order.setStatus(Order.OrderStatus.BILLED);
        order.setBill(bill);
        orderRepository.save(order);

        return billRepository.save(bill);
    }

    public Payment processPayment(Long billId, Payment.PaymentMethod method) {
        Long safeBillId = Objects.requireNonNull(billId, "billId is required");
        Bill bill = billRepository.findById(safeBillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));

        if (bill.getPayment() != null) {
            return bill.getPayment();
        }

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setPaymentMethod(method == null ? Payment.PaymentMethod.UPI : method);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);

        Payment savedPayment = paymentRepository.save(payment);

        Order order = bill.getOrder();
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        bill.setPayment(savedPayment);
        billRepository.save(bill);

        return savedPayment;
    }

    @Transactional(readOnly = true)
    public List<Bill> customerBillHistory(String customerPhone) {
        String safeCustomerPhone = Objects.requireNonNull(customerPhone, "customerPhone is required");
        return billRepository.findByOrderCustomerPhoneOrderByGeneratedAtDesc(safeCustomerPhone);
    }

    private BigDecimal sanitizeRate(BigDecimal rate) {
        BigDecimal safe = rate == null ? BigDecimal.ZERO : rate;
        if (safe.compareTo(BigDecimal.ZERO) < 0 || safe.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rate must be between 0 and 100");
        }
        return safe;
    }

    public record OrderLineCommand(Long medicineId, Integer quantity) {
    }
}
