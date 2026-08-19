package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.Order;
import com.pharmacy.model.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BillFactory {

    // Factory Pattern: pure construction. BillingFacade resolves subtotal/discount/tax/total
    // via the Strategy + Decorator pipeline before handing them here.
    public Bill createBill(Order order, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal total) {
        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setSubtotal(subtotal);
        bill.setDiscountAmount(discountAmount);
        bill.setTaxAmount(taxAmount);
        bill.setTotal(total);
        return bill;
    }

    public Invoice createInvoice(Supplier supplier, BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setSupplier(supplier);
        invoice.setAmount(amount);
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        return invoice;
    }
}
