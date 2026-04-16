package com.pharmacy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_suppliers")
public class Supplier extends User {

    @Override
    public String roleName() {
        return "SUPPLIER";
    }
}
