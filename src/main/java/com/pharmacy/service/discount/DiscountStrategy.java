package com.pharmacy.service.discount;

import com.pharmacy.model.Customer;

import java.math.BigDecimal;

// OCP: new discount behavior can be introduced by adding new strategy implementations.
public interface DiscountStrategy {

    boolean supports(Customer customer);

    BigDecimal discountPercent(Customer customer);
}
