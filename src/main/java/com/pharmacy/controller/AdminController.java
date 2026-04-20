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
        model.addAttribute("medicines", adminService.listMedicines());
        model.addAttribute("inventories", inventoryRepository.findAll());
        model.addAttribute("alerts", inventoryObserver.latestAlerts());
        model.addAttribute("automationActions", adminService.latestAutomationActions());
        return "dashboard/admin";
    }

    @PostMapping("/admin/medicine/save")
    public String saveMedicine(
            @RequestParam("inventoryId") Long inventoryId,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("manufacturer") String manufacturer,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQty") Integer stockQty,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            RedirectAttributes redirectAttributes
    ) {
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setCategory(category);
        medicine.setManufacturer(manufacturer);
        medicine.setPrice(price);
        medicine.setStockQty(stockQty);
        medicine.setExpiryDate(expiryDate);
        medicine.setLowStockThreshold(InventoryObserver.LOW_STOCK_THRESHOLD);
        medicine.setMedicineType(Medicine.MedicineType.OTHER);

        adminService.manageMedicineInventory(medicine, inventoryId);
        redirectAttributes.addFlashAttribute("successMessage", "Medicine saved successfully.");
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
}
