package com.pharmacy.controller.api;

import com.pharmacy.domain.Bill;
import com.pharmacy.domain.Order;
import com.pharmacy.domain.Payment;
import com.pharmacy.service.billing.BillingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@Validated
public class BillingApiController {

    private final BillingService billingService;

    public BillingApiController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public Order placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        List<BillingService.OrderLineCommand> lines = request.items().stream()
                .map(item -> new BillingService.OrderLineCommand(item.medicineId(), item.quantity()))
                .toList();
        return billingService.placeOrder(request.customerId(), lines);
    }

    @PostMapping("/bills/{orderId}")
    public Bill generateBill(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody GenerateBillRequest request
    ) {
        return billingService.generateBill(orderId, request.taxPercent(), request.discountPercent());
    }

    @PostMapping("/payments/{billId}")
    public Payment processPayment(
            @PathVariable("billId") Long billId,
            @Valid @RequestBody PaymentRequest request
    ) {
        return billingService.processPayment(billId, request.paymentMethod());
    }

    @GetMapping("/history/{customerId}")
    public List<Bill> history(@PathVariable("customerId") Long customerId) {
        return billingService.customerBillHistory(customerId);
    }

    public record PlaceOrderRequest(
            @NotNull Long customerId,
            @NotEmpty List<@Valid OrderLineItemRequest> items
    ) {
    }

    public record OrderLineItemRequest(@NotNull Long medicineId, @NotNull @Min(1) Integer quantity) {
    }

    public record GenerateBillRequest(
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxPercent,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal discountPercent
    ) {
    }

    public record PaymentRequest(@NotNull Payment.PaymentMethod paymentMethod) {
    }
}
