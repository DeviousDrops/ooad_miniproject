package com.pharmacy.pattern.factory;

import com.pharmacy.domain.Medicine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class SyrupMedicineFactory implements MedicineFactory {

    @Override
    public Medicine createMedicine(
            String name,
            String category,
            BigDecimal price,
            Integer stockQty,
            LocalDate expiryDate,
            Integer lowStockThreshold
    ) {
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setCategory(category == null || category.isBlank() ? "Syrup" : category);
        medicine.setPrice(price);
        medicine.setStockQty(stockQty);
        medicine.setExpiryDate(expiryDate);
        medicine.setLowStockThreshold(lowStockThreshold);
        medicine.setMedicineType(Medicine.MedicineType.SYRUP);
        return medicine;
    }
}
