package com.pharmacy.service.discount;

import com.pharmacy.model.Customer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class LoyaltyDiscountStrategy implements DiscountStrategy {

    @Override
    public boolean supports(Customer customer) {
        return customer != null && customer.getLoyaltyPoints() != null && customer.getLoyaltyPoints() > 0;
    }

    @Override
    public BigDecimal discountPercent(Customer customer) {
        int points = customer.getLoyaltyPoints() == null ? 0 : customer.getLoyaltyPoints();
        if (points >= 200) {
            return BigDecimal.valueOf(15);
        }
        if (points >= 100) {
            return BigDecimal.valueOf(10);
        }
        return BigDecimal.valueOf(5);
    }
}
