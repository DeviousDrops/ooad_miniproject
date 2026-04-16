package com.pharmacy.pattern.decorator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class BillAmountDecorator implements BillAmountComponent {

    protected final BillAmountComponent wrapped;

    protected BillAmountDecorator(BillAmountComponent wrapped) {
        this.wrapped = wrapped;
    }

    protected BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
