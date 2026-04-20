package com.pharmacy.controller.api;

import com.pharmacy.model.Inventory;
import com.pharmacy.model.Medicine;
import com.pharmacy.service.inventory.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryApiController {

    private final InventoryService inventoryService;

    public InventoryApiController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/medicines")
    public List<Medicine> listMedicines(@RequestParam(name = "query", required = false) String query) {
        return inventoryService.searchMedicines(query);
    }

    @PostMapping("/medicines")
    @ResponseStatus(HttpStatus.CREATED)
    public Medicine manageMedicine(@Valid @RequestBody ManageMedicineRequest request) {
        return inventoryService.manageMedicineInventory(new InventoryService.ManageMedicineCommand(
                request.inventoryId(),
                request.medicineId(),
                request.name(),
                request.category(),
                request.price(),
                request.stockQty(),
                request.medicineType(),
                request.expiryDate(),
                request.lowStockThreshold()
        ));
    }

    @PutMapping("/status/{inventoryId}")
    public Inventory updateStatus(@PathVariable("inventoryId") Long inventoryId) {
        return inventoryService.updateInventoryStatus(inventoryId);
    }

    @GetMapping("/alerts")
    public List<String> alerts() {
        return inventoryService.latestAlerts();
    }

    @GetMapping("/verify-stock")
    public boolean verifyStock(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("qty") @Min(1) Integer qty
    ) {
        return inventoryService.verifyStock(medicineId, qty);
    }

    @GetMapping("/default")
    public Inventory defaultInventory() {
        return inventoryService.getDefaultInventory();
    }

    public record ManageMedicineRequest(
            @NotNull Long inventoryId,
            Long medicineId,
            @NotBlank String name,
            @NotBlank String category,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotNull @Min(0) Integer stockQty,
            @NotBlank String medicineType,
            @NotNull @Future LocalDate expiryDate,
            @NotNull @Min(1) Integer lowStockThreshold
    ) {
    }
}
