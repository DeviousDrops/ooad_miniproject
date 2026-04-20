package com.pharmacy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_bills")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long billId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @OneToOne(mappedBy = "bill")
    private Payment payment;

    @PrePersist
    void onCreate() {
        generatedAt = LocalDateTime.now();
    }

    public Bill generateBill(Order sourceOrder) {
        this.order = sourceOrder;
        this.subtotal = sourceOrder == null ? BigDecimal.ZERO : sourceOrder.calculateSubtotal();
        this.taxAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.discountApplied = BigDecimal.ZERO;
        this.total = subtotal;
        return this;
    }

    public void applyDiscount(double percent) {
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        BigDecimal pct = BigDecimal.valueOf(Math.max(0.0d, percent)).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        discountApplied = subtotal.multiply(pct).setScale(2, RoundingMode.HALF_UP);
        discountAmount = discountApplied;
        total = subtotal.subtract(discountApplied).add(taxAmount == null ? BigDecimal.ZERO : taxAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public void printBill() {
        // The web layer renders printable bill views; this method keeps the domain API complete.
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountApplied() {
        return discountApplied;
    }

    public void setDiscountApplied(BigDecimal discountApplied) {
        this.discountApplied = discountApplied;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}
