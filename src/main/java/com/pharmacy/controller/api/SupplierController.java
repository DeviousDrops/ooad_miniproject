package com.pharmacy.controller.api;

import com.pharmacy.domain.Invoice;
import com.pharmacy.domain.Shipment;
import com.pharmacy.service.actor.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/supplier")
@Validated
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping("/restock")
    public Shipment supplyRestock(@Valid @RequestBody RestockRequest request) {
        return supplierService.supplyRestock(
                request.supplierId(),
                request.inventoryId(),
                request.medicineId(),
                request.quantity(),
                request.expectedDate()
        );
    }

    @PostMapping("/verify-shipment/{shipmentId}")
    public Shipment verifyShipment(@PathVariable("shipmentId") Long shipmentId) {
        return supplierService.shipmentVerification(shipmentId);
    }

    @PostMapping("/digital-invoice")
    public Invoice digitalInvoice(@Valid @RequestBody DigitalInvoiceRequest request) {
        return supplierService.submitDigitalInvoice(
                request.supplierId(),
                request.shipmentId(),
                request.invoiceNumber(),
                request.amount()
        );
    }

    public record RestockRequest(
            @NotNull Long supplierId,
            @NotNull Long inventoryId,
            @NotNull Long medicineId,
            @NotNull @Min(1) Integer quantity,
            @NotNull @Future LocalDate expectedDate
    ) {
    }

    public record DigitalInvoiceRequest(
            @NotNull Long supplierId,
            @NotNull Long shipmentId,
            @NotBlank String invoiceNumber,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {
    }
}
