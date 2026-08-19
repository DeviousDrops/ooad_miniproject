package com.pharmacy.service.discount;

import com.pharmacy.model.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscountStrategyTest {

    private final LoyaltyDiscountStrategy loyalty = new LoyaltyDiscountStrategy();
    private final DefaultDiscountStrategy fallback = new DefaultDiscountStrategy();

    private Customer withPoints(int points) {
        Customer customer = new Customer();
        customer.setLoyaltyPoints(points);
        return customer;
    }

    @Test
    void loyaltyDoesNotSupportZeroPoints() {
        assertFalse(loyalty.supports(withPoints(0)));
        assertTrue(fallback.supports(withPoints(0)));
    }

    @Test
    void loyaltyGivesFivePercentBelowOneHundredPoints() {
        assertTrue(loyalty.supports(withPoints(99)));
        assertEquals(BigDecimal.valueOf(5), loyalty.discountPercent(withPoints(99)));
    }

    @Test
    void loyaltyGivesTenPercentAtOneHundredPoints() {
        assertEquals(BigDecimal.valueOf(10), loyalty.discountPercent(withPoints(100)));
        assertEquals(BigDecimal.valueOf(10), loyalty.discountPercent(withPoints(199)));
    }

    @Test
    void loyaltyGivesFifteenPercentAtTwoHundredPoints() {
        assertEquals(BigDecimal.valueOf(15), loyalty.discountPercent(withPoints(200)));
        assertEquals(BigDecimal.valueOf(15), loyalty.discountPercent(withPoints(500)));
    }

    @Test
    void defaultStrategySupportsEveryoneAndGivesNoDiscount() {
        assertTrue(fallback.supports(null));
        assertTrue(fallback.supports(withPoints(1000)));
        assertEquals(BigDecimal.ZERO, fallback.discountPercent(withPoints(1000)));
    }
}
