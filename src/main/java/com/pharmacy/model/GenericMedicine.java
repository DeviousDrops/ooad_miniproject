package com.pharmacy.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("OTHER")
public class GenericMedicine extends Medicine {

    @Override
    public int nearExpiryWindowDays() {
        return 30;
    }

    @Override
    public String unitOfSale() {
        return "unit";
    }
}
