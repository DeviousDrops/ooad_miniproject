package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.model.OrderItem;
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.PaymentRepository;
import com.pharmacy.service.discount.DiscountStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BillingFacadeTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private BillFactory billFactory;

    @Mock
    private InventoryObserver inventoryObserver;

    @Mock
    private DiscountStrategy discountStrategy;

    @Test
    void processCustomerBilling_shouldVerifyStockAndPersistBill() {
        BillingFacade billingFacade = new BillingFacade(
                orderRepository,
                billRepository,
                paymentRepository,
                medicineRepository,
                billFactory,
                inventoryObserver,
                List.of(discountStrategy)
        );

        Customer customer = new Customer();
        customer.setLoyaltyPoints(120);

        Medicine medicine = new Medicine();
        medicine.setName("Paracetamol");
        medicine.setStockQty(50);
        medicine.setPrice(new BigDecimal("5.00"));

        OrderItem item = new OrderItem();
        item.setMedicine(medicine);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("5.00"));
        item.setLineTotal(new BigDecimal("10.00"));

        Order order = new Order();
        order.setOrderId(1L);
        order.setCustomer(customer);
        order.addItem(item);

        Bill generatedBill = new Bill();
        generatedBill.setOrder(order);
        generatedBill.setTotal(new BigDecimal("9.00"));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(discountStrategy.supports(customer)).thenReturn(true);
        when(discountStrategy.discountPercent(customer)).thenReturn(new BigDecimal("10"));
        when(billFactory.createBill(order, new BigDecimal("10"))).thenReturn(generatedBill);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Bill saved = billingFacade.processCustomerBilling(1L);

        assertEquals(Order.OrderStatus.BILLED, order.getStatus());
        assertEquals(generatedBill, saved);
        verify(medicineRepository).save(medicine);
        verify(inventoryObserver).checkLowStock(medicine);
        verify(billRepository).save(generatedBill);
    }
}
