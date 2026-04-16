package com.pharmacy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_pharmacists")
public class Pharmacist extends User {

    @Override
    public String roleName() {
        return "PHARMACIST";
    }
}
