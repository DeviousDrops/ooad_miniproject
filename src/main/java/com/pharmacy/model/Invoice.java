package com.pharmacy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "app_procurement_invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Shipment> shipments = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private LocalDateTime invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    private LocalDateTime paidAt;

    @PrePersist
    void onCreate() {
        submittedAt = LocalDateTime.now();
        invoiceDate = submittedAt;
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
    }

    public enum PaymentStatus {
        PENDING,
        PROCESSED
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDateTime invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        items.add(item);
    }

    public String getItemSummary() {
        if (items == null || items.isEmpty()) {
            return "No medicines";
        }
        return items.stream()
                .map(item -> item.getMedicine().getName() + " x" + item.getQuantity())
                .reduce((left, right) -> left + ", " + right)
                .orElse("No medicines");
    }

    public Integer getItemCount() {
        return items == null ? 0 : items.size();
    }

    public List<Shipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<Shipment> shipments) {
        this.shipments = shipments;
    }

    public String getDeliveryStatusSummary() {
        if (shipments == null || shipments.isEmpty()) {
            return "No shipments";
        }
        boolean allDelivered = shipments.stream()
                .allMatch(shipment -> shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED);
        return allDelivered ? "Delivered" : "In Transit";
    }

    public String getShipmentSummary() {
        if (shipments == null || shipments.isEmpty()) {
            return "No shipments";
        }
        return shipments.stream()
                .map(shipment -> "#" + shipment.getShipmentId() + " " + shipment.getMedicine().getName())
                .collect(Collectors.joining(", "));
    }
}
