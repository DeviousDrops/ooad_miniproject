package com.pharmacy.repository;

import com.pharmacy.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerUserId(Long userId);

    List<Order> findByCustomerPhoneOrderByOrderedAtDesc(String phone);

    List<Order> findByStatusOrderByOrderedAtAsc(Order.OrderStatus status);
}
