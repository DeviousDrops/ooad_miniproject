package com.pharmacy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false, unique = true)
    private Bill bill;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @PrePersist
    void onCreate() {
        paidAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.SUCCESS;
        }
        if (amount == null && bill != null) {
            amount = bill.getTotal();
        }
    }

    public boolean processPayment() {
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.CASH;
        }
        if (amount == null && bill != null) {
            amount = bill.getTotal();
        }
        status = validateTransaction() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        return status == PaymentStatus.SUCCESS;
    }

    public boolean validateTransaction() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 && paymentMethod != null;
    }

    public enum PaymentMethod {
        CASH,
        CARD,
        UPI
    }

    public enum PaymentStatus {
        SUCCESS,
        FAILED,
        PENDING
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
