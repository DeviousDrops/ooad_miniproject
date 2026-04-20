package com.pharmacy.service.discount;

import com.pharmacy.model.Customer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(99)
public class DefaultDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean supports(Customer customer) {
        return true;
    }

    @Override
    public BigDecimal discountPercent(Customer customer) {
        return BigDecimal.ZERO;
    }
}
