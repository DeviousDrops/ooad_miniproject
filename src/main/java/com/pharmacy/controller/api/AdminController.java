package com.pharmacy.controller.api;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.Report;
import com.pharmacy.service.actor.AdminService;
import com.pharmacy.service.inventory.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/manage-medicine")
    public Medicine manageMedicine(@Valid @RequestBody ManageMedicineRequest request) {
        return adminService.manageMedicineInventory(new InventoryService.ManageMedicineCommand(
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

    @GetMapping("/sales-analytics")
    public Report salesAnalytics(Authentication authentication) {
        String actor = authentication == null ? "admin" : authentication.getName();
        return adminService.generateSalesAnalytics(actor);
    }

    @GetMapping("/automate-supply")
    public List<AdminService.SupplyAutomationItem> automateSupply() {
        return adminService.automateMedicineSupply();
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
