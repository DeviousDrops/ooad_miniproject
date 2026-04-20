package com.pharmacy.controller.api;

import com.pharmacy.model.Inventory;
import com.pharmacy.model.Payment;
import com.pharmacy.service.actor.PharmacistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pharmacist")
@Validated
public class PharmacistController {

    private final PharmacistService pharmacistService;

    public PharmacistController(PharmacistService pharmacistService) {
        this.pharmacistService = pharmacistService;
    }

    @GetMapping("/verify-stock")
    public boolean verifyStock(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("qty") @Min(1) Integer qty
    ) {
        return pharmacistService.verifyStock(medicineId, qty);
    }

    @PostMapping("/process-billing/{orderId}")
    public PharmacistService.BillingReceipt processBilling(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody ProcessBillingRequest request
    ) {
        return pharmacistService.processCustomerBilling(
                orderId,
                request.taxPercent(),
                request.discountPercent(),
                request.paymentMethod()
        );
    }

    @PutMapping("/update-inventory-status/{inventoryId}")
    public Inventory updateInventory(@PathVariable("inventoryId") Long inventoryId) {
        return pharmacistService.updateInventoryStatus(inventoryId);
    }

    public record ProcessBillingRequest(
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal taxPercent,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal discountPercent,
            @NotNull Payment.PaymentMethod paymentMethod
    ) {
    }
}
