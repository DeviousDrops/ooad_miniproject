package com.pharmacy;

import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.OrderRepository;
import com.pharmacy.service.CustomerService;
import com.pharmacy.service.PharmacistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Characterization tests: pin behaviour that must survive the v2 refactor.
@SpringBootTest
class BillingGuardsTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PharmacistService pharmacistService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void billingDeductsStockAndAdvancesOrderStatus() {
        String customerPhone = customerRepository.findAll().get(0).getPhone();
        Medicine medicine = medicineRepository.findAll().get(0);
        int stockBefore = medicine.getStockQty();

        Order order = customerService.placeOrder(
                customerPhone,
                List.of(new CustomerService.OrderRequestItem(medicine.getMedicineId(), 2))
        );
        assertEquals(Order.OrderStatus.CREATED, order.getStatus());

        pharmacistService.processCustomerBilling(order.getOrderId());

        Medicine afterBilling = medicineRepository.findById(medicine.getMedicineId()).orElseThrow();
        assertEquals(stockBefore - 2, afterBilling.getStockQty());
        assertEquals(Order.OrderStatus.BILLED, orderRepository.findById(order.getOrderId()).orElseThrow().getStatus());

        customerService.makePayment(
                customerPhone,
                orderRepository.findById(order.getOrderId()).orElseThrow().getBill().getBillId(),
                com.pharmacy.model.Payment.PaymentMethod.CASH
        );
        assertEquals(Order.OrderStatus.PAID, orderRepository.findById(order.getOrderId()).orElseThrow().getStatus());
    }

    @Test
    void payingAnotherCustomersBillThrows() {
        String owner = customerRepository.findAll().get(0).getPhone();
        String otherCustomer = createCustomer("9000000099").getPhone();
        Medicine medicine = medicineRepository.findAll().get(0);

        Order order = customerService.placeOrder(
                owner,
                List.of(new CustomerService.OrderRequestItem(medicine.getMedicineId(), 1))
        );
        pharmacistService.processCustomerBilling(order.getOrderId());
        Long billId = orderRepository.findById(order.getOrderId()).orElseThrow().getBill().getBillId();

        assertThrows(IllegalStateException.class, () -> customerService.makePayment(
                otherCustomer,
                billId,
                com.pharmacy.model.Payment.PaymentMethod.CASH
        ));
    }

    private Customer createCustomer(String phone) {
        Customer customer = new Customer();
        customer.setName("Other Customer " + phone);
        customer.setEmail(phone + "@customer.test.local");
        customer.setPhone(phone);
        customer.setUsername(phone);
        customer.setPassword("irrelevant");
        customer.setCustomerId(Long.valueOf(phone));
        return customerRepository.save(customer);
    }

    @Test
    void billingSameOrderTwiceThrows() {
        String customerPhone = customerRepository.findAll().get(0).getPhone();
        Medicine medicine = medicineRepository.findAll().get(0);

        Order order = customerService.placeOrder(
                customerPhone,
                List.of(new CustomerService.OrderRequestItem(medicine.getMedicineId(), 1))
        );
        pharmacistService.processCustomerBilling(order.getOrderId());

        assertThrows(IllegalStateException.class, () -> pharmacistService.processCustomerBilling(order.getOrderId()));
    }

    @Test
    void insufficientStockThrowsOnOrderPlacement() {
        Customer customer = customerRepository.findAll().get(0);
        Medicine medicine = medicineRepository.findAll().get(0);

        assertThrows(IllegalStateException.class, () -> customerService.placeOrder(
                customer.getPhone(),
                List.of(new CustomerService.OrderRequestItem(medicine.getMedicineId(), medicine.getStockQty() + 1000))
        ));
    }

    @Test
    void cancelledOrderCannotBeBilled() {
        String customerPhone = customerRepository.findAll().get(0).getPhone();
        Medicine medicine = medicineRepository.findAll().get(0);

        Order order = customerService.placeOrder(
                customerPhone,
                List.of(new CustomerService.OrderRequestItem(medicine.getMedicineId(), 1))
        );
        customerService.cancelOrder(customerPhone, order.getOrderId());

        assertThrows(IllegalStateException.class, () -> pharmacistService.processCustomerBilling(order.getOrderId()));
    }
}
