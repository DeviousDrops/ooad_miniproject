package com.pharmacy.repository;

import com.pharmacy.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerId(Long customerId);

    @Query("select c from Customer c where c.phone = :phone")
    Optional<Customer> findByPhone(@Param("phone") String phone);

    @Query("select c from Customer c where lower(c.name) = lower(:name) order by c.userId asc")
    Optional<Customer> findFirstByNameIgnoreCase(@Param("name") String name);
}
