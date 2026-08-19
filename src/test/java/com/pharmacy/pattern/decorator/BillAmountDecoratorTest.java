package com.pharmacy.pattern.decorator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillAmountDecoratorTest {

    @Test
    void discountThenTaxAppliesDiscountBeforeTax() {
        BillAmountComponent chain = new TaxDecorator(
                new DiscountDecorator(new BaseBillAmount(BigDecimal.valueOf(100)), BigDecimal.valueOf(10)),
                BigDecimal.valueOf(5)
        );

        // 100 - 10% discount = 90, then + 5% tax on 90 = 94.50
        assertEquals(new BigDecimal("94.50"), chain.calculateTotal());
    }

    @Test
    void compositionOrderChangesTheResult() {
        BillAmountComponent discountThenTax = new TaxDecorator(
                new DiscountDecorator(new BaseBillAmount(BigDecimal.valueOf(17)), BigDecimal.valueOf(12.5)),
                BigDecimal.valueOf(8.3)
        );
        BillAmountComponent taxThenDiscount = new DiscountDecorator(
                new TaxDecorator(new BaseBillAmount(BigDecimal.valueOf(17)), BigDecimal.valueOf(8.3)),
                BigDecimal.valueOf(12.5)
        );

        // Per-step rounding makes the two orders diverge: discount-then-tax is the pipeline BillingFacade uses.
        assertEquals(new BigDecimal("16.10"), discountThenTax.calculateTotal());
        assertEquals(new BigDecimal("16.11"), taxThenDiscount.calculateTotal());
    }

    @Test
    void zeroDiscountAndZeroTaxLeavesSubtotalUnchanged() {
        BillAmountComponent chain = new TaxDecorator(
                new DiscountDecorator(new BaseBillAmount(BigDecimal.valueOf(50)), BigDecimal.ZERO),
                BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("50.00"), chain.calculateTotal());
    }

    @Test
    void nullSubtotalNormalizesToZero() {
        BillAmountComponent base = new BaseBillAmount(null);

        assertEquals(new BigDecimal("0.00"), base.calculateTotal());
    }
}
