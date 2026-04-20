package com.pharmacy.repository;

import com.pharmacy.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

	Optional<Supplier> findBySupplierId(Long supplierId);
}
