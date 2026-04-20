package com.pharmacy.repository;

import com.pharmacy.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findTop10ByOrderByGeneratedAtDesc();
}
