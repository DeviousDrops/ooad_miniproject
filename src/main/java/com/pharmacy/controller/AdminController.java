package com.pharmacy.controller;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.Report;
import com.pharmacy.service.AdminService;
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

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(Model model) {
        List<Medicine> medicines = adminService.listMedicines();
        model.addAttribute("medicines", medicines);
        model.addAttribute("invoices", adminService.listInvoices());
        model.addAttribute("alerts", adminService.currentLowStockAlerts());
        model.addAttribute("shipments", adminService.listShipments());
        return "dashboard/admin";
    }

    @PostMapping("/admin/medicine/save")
    public String saveMedicine(
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
            adminService.createMedicine(medicineType, name, category, manufacturer, price, stockQty, expiryDate, lowStockThreshold);
            redirectAttributes.addFlashAttribute("successMessage", "Medicine added successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to save medicine. Please verify all medicine details.");
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/medicine/update")
    public String updateMedicine(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("name") String name,
            @RequestParam("category") String category,
            @RequestParam("manufacturer") String manufacturer,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQty") Integer stockQty,
            @RequestParam("expiryDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(value = "lowStockThreshold", defaultValue = "10") Integer lowStockThreshold,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.updateMedicine(medicineId, name, category, manufacturer, price, stockQty, expiryDate, lowStockThreshold);
            redirectAttributes.addFlashAttribute("successMessage", "Medicine updated successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to update medicine. Please verify all medicine details.");
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/medicine/delete")
    public String deleteMedicine(
            @RequestParam("medicineId") Long medicineId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.deleteMedicine(medicineId);
            redirectAttributes.addFlashAttribute("successMessage", "Medicine deleted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/report/sales")
    public String salesReport(RedirectAttributes redirectAttributes) {
        try {
            Report report = adminService.generateSalesAnalytics();
            redirectAttributes.addFlashAttribute("infoMessage", report.getSummary());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/report/inventory")
    public String inventoryReport(RedirectAttributes redirectAttributes) {
        try {
            Report report = adminService.generateInventoryReport();
            redirectAttributes.addFlashAttribute("infoMessage", report.getSummary());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/bills/pay")
    public String payBill(
            @RequestParam("invoiceId") Long invoiceId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.payInvoice(invoiceId);
            redirectAttributes.addFlashAttribute("successMessage", "Bill paid successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/admin";
    }

    @PostMapping("/admin/bills/decline")
    public String declineBill(
            @RequestParam("invoiceId") Long invoiceId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            adminService.declineInvoice(invoiceId);
            redirectAttributes.addFlashAttribute("successMessage", "Bill declined successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/admin";
    }
}
