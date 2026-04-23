package com.pharmacy.controller.api;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.Shipment;
import com.pharmacy.service.actor.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    @PostMapping("/bills")
    public Invoice createBill(@Valid @RequestBody SupplierBillRequest request) {
        return supplierService.generateSupplierBill(
                request.supplierId(),
                request.medicineIds(),
                request.quantities()
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

    public record SupplierBillRequest(
            @NotNull Long supplierId,
            @NotNull List<Long> medicineIds,
            @NotNull List<Integer> quantities
    ) {
    }
}
