package com.pharmacy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_customers")
public class Customer extends User {

    @Override
    public String roleName() {
        return "CUSTOMER";
    }
}
