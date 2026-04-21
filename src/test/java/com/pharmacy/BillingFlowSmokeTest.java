package com.pharmacy;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.service.CustomerService;
import com.pharmacy.service.PharmacistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BillingFlowSmokeTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PharmacistService pharmacistService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private MedicineRepository medicineRepository;

    @Test
    void pharmacistCanProcessNewOrder() {
        Long customerRef = customerRepository.findAll().get(0).getCustomerId();
        Medicine medicine = medicineRepository.findAll().get(0);

        Order order = customerService.placeOrder(
                customerRef,
                List.of(new CustomerService.OrderRequestItem(medicine.getMedicineId(), 1))
        );

        assertNotNull(pharmacistService.processCustomerBilling(order.getOrderId()));
    }
}
