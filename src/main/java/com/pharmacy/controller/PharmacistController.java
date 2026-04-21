package com.pharmacy.controller;

import com.pharmacy.model.Bill;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.service.PharmacistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller("webPharmacistController")
@PreAuthorize("hasRole('PHARMACIST')")
public class PharmacistController {

    private static final Logger logger = LoggerFactory.getLogger(PharmacistController.class);

    private final PharmacistService pharmacistService;
    private final MedicineRepository medicineRepository;

    public PharmacistController(PharmacistService pharmacistService, MedicineRepository medicineRepository) {
        this.pharmacistService = pharmacistService;
        this.medicineRepository = medicineRepository;
    }

    @GetMapping("/dashboard/pharmacist")
    public String pharmacistDashboard(Model model) {
        model.addAttribute("medicines", medicineRepository.findAll());
        model.addAttribute("unprocessedOrders", pharmacistService.findUnprocessedOrders());
        model.addAttribute("processedBills", pharmacistService.findProcessedBills());
        return "dashboard/pharmacist";
    }

    @PostMapping("/pharmacist/verify-stock")
    public String verifyStock(
            @RequestParam("medicineId") Long medicineId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean available = pharmacistService.verifyStock(medicineId);
            redirectAttributes.addFlashAttribute("infoMessage", available
                    ? "Stock is available for medicine ID " + medicineId
                    : "Stock is not available for medicine ID " + medicineId);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/pharmacist";
    }

    @PostMapping("/pharmacist/process-billing")
    public String processBilling(
            @RequestParam("orderId") Long orderId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Bill bill = pharmacistService.processCustomerBilling(orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Billing completed. Bill ID: " + bill.getBillId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected billing failure for orderId={}", orderId, ex);
            redirectAttributes.addFlashAttribute("errorMessage", "Unexpected billing error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
        }
        return "redirect:/dashboard/pharmacist";
    }

    @PostMapping("/pharmacist/update-inventory")
    public String updateInventory(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("qty") Integer qty,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pharmacistService.updateInventoryStatus(medicineId, qty);
            redirectAttributes.addFlashAttribute("successMessage", "Inventory adjusted successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/pharmacist";
    }
}
