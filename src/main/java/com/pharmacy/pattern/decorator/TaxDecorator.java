package com.pharmacy.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TaxDecorator extends BillAmountDecorator {

    private final BigDecimal taxPercent;

    public TaxDecorator(BillAmountComponent wrapped, BigDecimal taxPercent) {
        super(wrapped);
        this.taxPercent = taxPercent == null ? BigDecimal.ZERO : taxPercent;
    }

    @Override
    public BigDecimal calculateTotal() {
        BigDecimal amount = wrapped.calculateTotal();
        BigDecimal tax = amount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return normalize(amount.add(tax));
    }
}
