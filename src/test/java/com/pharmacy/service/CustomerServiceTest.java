package com.pharmacy.service;

import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.model.Payment;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.repository.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CustomerServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private BillingFacade billingFacade;

    @Test
    void placeOrder_shouldCreateOrderWithCalculatedTotal() {
        CustomerService customerService = new CustomerService(
                medicineRepository,
                customerRepository,
                orderRepository,
                prescriptionRepository,
                billingFacade
        );

        Customer customer = new Customer();
        customer.setUserId(1L);

        Medicine medicine = new Medicine();
        medicine.setMedicineId(2L);
        medicine.setName("Ibuprofen");
        medicine.setStockQty(20);
        medicine.setPrice(new BigDecimal("12.50"));

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(medicineRepository.findById(2L)).thenReturn(Optional.of(medicine));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = customerService.placeOrder(
                1L,
                List.of(new CustomerService.OrderRequestItem(2L, 3))
        );

        assertNotNull(order);
        assertEquals(1, order.getItems().size());
        assertEquals(new BigDecimal("37.50"), order.getTotalAmount());
        verify(orderRepository).save(order);
    }

    @Test
    void makePayment_shouldDelegateToBillingFacade() {
        CustomerService customerService = new CustomerService(
                medicineRepository,
                customerRepository,
                orderRepository,
                prescriptionRepository,
                billingFacade
        );

        Payment payment = new Payment();
        payment.setPaymentMethod(Payment.PaymentMethod.UPI);

        when(billingFacade.processPayment(5L, Payment.PaymentMethod.UPI)).thenReturn(payment);

        Payment actual = customerService.makePayment(5L, Payment.PaymentMethod.UPI);

        assertEquals(Payment.PaymentMethod.UPI, actual.getPaymentMethod());
        verify(billingFacade).processPayment(5L, Payment.PaymentMethod.UPI);
    }
}
