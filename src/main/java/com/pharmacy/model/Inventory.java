package com.pharmacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @Column
    private Long medicineId;

    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Medicine> medicines = new ArrayList<>();

    @PrePersist
    void onCreate() {
        lastUpdated = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    public void addMedicine(Medicine medicine) {
        medicine.setInventory(this);
        medicines.add(medicine);
        medicineId = medicine.getMedicineId();
        quantity = medicines.stream().map(Medicine::getStockQty).filter(q -> q != null).mapToInt(Integer::intValue).sum();
        touch();
    }

    public void updateInventory(int qty) {
        quantity = Math.max(0, qty);
        touch();
    }

    public boolean checkLowStock() {
        return quantity < 10;
    }

    public void touch() {
        lastUpdated = LocalDateTime.now();
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public List<Medicine> getMedicines() {
        return medicines;
    }
}
