package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.Order;
import com.pharmacy.model.Shipment;
import com.pharmacy.model.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
public class BillFactory {

    // Factory Pattern: centralized object construction for billing artifacts.
    public Bill createBill(Order order, BigDecimal discountPercent) {
        BigDecimal subtotal = order.calculateSubtotal().setScale(2, RoundingMode.HALF_UP);
        BigDecimal safePercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal discountAmount = subtotal.multiply(safePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setSubtotal(subtotal);
        bill.setTaxAmount(BigDecimal.ZERO);
        bill.setDiscountApplied(discountAmount);
        bill.setDiscountAmount(discountAmount);
        bill.setTotal(subtotal.subtract(discountAmount));
        return bill;
    }

    // Factory Pattern: the same factory can construct supplier invoices consistently.
    public Invoice createInvoice(Supplier supplier, Shipment shipment, BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setSupplier(supplier);
        invoice.setShipment(shipment);
        invoice.setAmount(amount);
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setInvoiceDate(LocalDateTime.now());
        return invoice;
    }
}
