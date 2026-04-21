package com.pharmacy.controller;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.Report;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.service.AdminService;
import com.pharmacy.service.InventoryObserver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller("webAdminController")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final InventoryRepository inventoryRepository;
    private final InventoryObserver inventoryObserver;

    public AdminController(AdminService adminService, InventoryRepository inventoryRepository, InventoryObserver inventoryObserver) {
        this.adminService = adminService;
        this.inventoryRepository = inventoryRepository;
        this.inventoryObserver = inventoryObserver;
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(Model model) {
        List<Medicine> medicines = adminService.listMedicines();
        model.addAttribute("medicines", medicines);
        model.addAttribute("inventories", inventoryRepository.findAll());
        model.addAttribute("defaultInventoryId", medicines.stream()
                .map(Medicine::getInventory)
                .filter(inventory -> inventory != null && inventory.getInventoryId() != null)
                .map(inventory -> inventory.getInventoryId().toString())
                .findFirst()
                .orElseGet(() -> inventoryRepository.findAll().stream()
                        .map(inventory -> inventory.getInventoryId().toString())
                        .findFirst()
                        .orElse("")));
        model.addAttribute("invoices", adminService.listInvoices());
        model.addAttribute("alerts", adminService.currentLowStockAlerts());
        model.addAttribute("shipments", adminService.listShipments());
        model.addAttribute("automationActions", adminService.latestAutomationActions());
        return "dashboard/admin";
    }

    @PostMapping("/admin/medicine/save")
    public String saveMedicine(
            @RequestParam(value = "medicineId", required = false) Long medicineId,
            @RequestParam("inventoryId") Long inventoryId,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("manufacturer") String manufacturer,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQty") Integer stockQty,
            @RequestParam(value = "medicineType", defaultValue = "OTHER") String medicineType,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "lowStockThreshold", defaultValue = "10") Integer lowStockThreshold,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Medicine medicine = new Medicine();
            medicine.setMedicineId(medicineId);
            medicine.setName(name);
            medicine.setCategory(category);
            medicine.setManufacturer(manufacturer);
            medicine.setPrice(price);
            medicine.setStockQty(stockQty);
            medicine.setExpiryDate(expiryDate);
            medicine.setLowStockThreshold(lowStockThreshold);
            medicine.setMedicineType(parseMedicineType(medicineType));

            adminService.manageMedicineInventory(medicine, inventoryId);
            redirectAttributes.addFlashAttribute("successMessage", medicineId == null
                    ? "Medicine added successfully."
                    : "Medicine updated successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save medicine. Please verify inventory ID and all medicine details.");
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/medicine/delete")
    public String deleteMedicine(
            @RequestParam("medicineId") Long medicineId,
            RedirectAttributes redirectAttributes
    ) {
        adminService.deleteMedicine(medicineId);
        redirectAttributes.addFlashAttribute("successMessage", "Medicine deleted successfully.");
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/report/sales")
    public String salesReport(RedirectAttributes redirectAttributes) {
        Report report = adminService.generateSalesAnalytics();
        redirectAttributes.addFlashAttribute("infoMessage", report.getSummary());
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/report/inventory")
    public String inventoryReport(RedirectAttributes redirectAttributes) {
        Report report = adminService.generateInventoryReport();
        redirectAttributes.addFlashAttribute("infoMessage", report.getSummary());
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/bills/pay")
    public String payBill(
            @RequestParam("invoiceId") Long invoiceId,
            RedirectAttributes redirectAttributes
    ) {
        adminService.payInvoice(invoiceId);
        redirectAttributes.addFlashAttribute("successMessage", "Bill paid successfully.");
        return "redirect:/dashboard/admin";
    }

    private Medicine.MedicineType parseMedicineType(String medicineType) {
        if (medicineType == null || medicineType.isBlank()) {
            return Medicine.MedicineType.OTHER;
        }
        try {
            return Medicine.MedicineType.valueOf(medicineType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Medicine.MedicineType.OTHER;
        }
    }
}
