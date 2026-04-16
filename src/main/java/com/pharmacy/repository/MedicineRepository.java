package com.pharmacy.repository;

import com.pharmacy.domain.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    List<Medicine> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);

    List<Medicine> findByStockQtyLessThanEqual(Integer stockQty);
}
