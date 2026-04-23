package com.pharmacy.repository;

import com.pharmacy.model.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByOrderCustomerUserId(Long userId);

    List<Bill> findByOrderCustomerPhoneOrderByGeneratedAtDesc(String phone);

    boolean existsByOrderOrderId(Long orderId);

    List<Bill> findAllByOrderByGeneratedAtDesc();
}
