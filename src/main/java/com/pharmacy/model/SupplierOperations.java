package com.pharmacy.model;

// ISP: supplier contract is independent from customer/pharmacist concerns.
public interface SupplierOperations {

    void supplyRestock(Medicine medicine, int qty);

    boolean shipmentVerification(long shipmentId);

    Invoice submitDigitalInvoice();
}
