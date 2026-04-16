package com.pharmacy.pattern.factory;

import com.pharmacy.domain.Medicine;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MedicineFactory {

    Medicine createMedicine(
            String name,
            String category,
            BigDecimal price,
            Integer stockQty,
            LocalDate expiryDate,
            Integer lowStockThreshold
    );
}
