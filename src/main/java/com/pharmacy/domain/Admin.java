package com.pharmacy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_admins")
public class Admin extends User {

    @Override
    public String roleName() {
        return "ADMIN";
    }
}
