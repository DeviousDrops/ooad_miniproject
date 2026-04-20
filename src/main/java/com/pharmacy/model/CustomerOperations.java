package com.pharmacy.model;

import java.util.List;

// ISP: customer-facing operations are separated from other actor capabilities.
public interface CustomerOperations {

    List<Medicine> searchMedicines(String name);

    Order placeOrder(Order order);

    List<Prescription> viewPrescriptionHistory();

    boolean makePayment(Payment payment);
}
