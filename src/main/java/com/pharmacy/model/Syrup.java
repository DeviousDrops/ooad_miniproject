package com.pharmacy.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SYRUP")
public class Syrup extends Medicine {

    // Liquids degrade faster once opened, so flag them earlier than solids.
    @Override
    public int nearExpiryWindowDays() {
        return 45;
    }

    @Override
    public String unitOfSale() {
        return "bottle";
    }
}
