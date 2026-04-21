package com.pharmacy.repository;

import com.pharmacy.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findAllByOrderByExpectedDateAsc();
}
