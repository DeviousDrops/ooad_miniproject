package com.pharmacy.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BaseBillAmount implements BillAmountComponent {

    private final BigDecimal subtotal;

    public BaseBillAmount(BigDecimal subtotal) {
        this.subtotal = normalize(subtotal);
    }

    @Override
    public BigDecimal calculateTotal() {
        return subtotal;
    }

    protected BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
