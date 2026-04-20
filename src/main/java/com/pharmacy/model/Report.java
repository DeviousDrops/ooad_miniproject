package com.pharmacy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportType reportType;

    @Column(nullable = false)
    private String generatedBy;

    @Column
    private String type;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Lob
    @Column(nullable = false)
    private String summary;

    @Lob
    @Column(nullable = false)
    private String data = "";

    @PrePersist
    void onCreate() {
        generatedAt = LocalDateTime.now();
        if (type == null && reportType != null) {
            type = reportType.name();
        }
        if (summary == null) {
            summary = "";
        }
        if (data == null) {
            data = summary;
        }
    }

    public enum ReportType {
        SALES,
        INVENTORY
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
        if (reportType != null) {
            this.type = reportType.name();
        }
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        this.data = summary;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
