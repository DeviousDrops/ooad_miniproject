package com.pharmacy.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DiscountDecorator extends BillAmountDecorator {

    private final BigDecimal discountPercent;

    public DiscountDecorator(BillAmountComponent wrapped, BigDecimal discountPercent) {
        super(wrapped);
        this.discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
    }

    @Override
    public BigDecimal calculateTotal() {
        BigDecimal amount = wrapped.calculateTotal();
        BigDecimal discount = amount.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return normalize(amount.subtract(discount));
    }
}
