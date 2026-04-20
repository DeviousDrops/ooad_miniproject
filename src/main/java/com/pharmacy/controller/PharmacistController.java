package com.pharmacy.controller;

import com.pharmacy.model.Bill;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.service.PharmacistService;
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

    private final PharmacistService pharmacistService;
    private final MedicineRepository medicineRepository;

    public PharmacistController(PharmacistService pharmacistService, MedicineRepository medicineRepository) {
        this.pharmacistService = pharmacistService;
        this.medicineRepository = medicineRepository;
    }

    @GetMapping("/dashboard/pharmacist")
    public String pharmacistDashboard(Model model) {
        model.addAttribute("medicines", medicineRepository.findAll());
        return "dashboard/pharmacist";
    }

    @PostMapping("/pharmacist/verify-stock")
    public String verifyStock(
            @RequestParam("medicineId") Long medicineId,
            RedirectAttributes redirectAttributes
    ) {
        boolean available = pharmacistService.verifyStock(medicineId);
        redirectAttributes.addFlashAttribute("infoMessage", available
                ? "Stock is available for medicine ID " + medicineId
                : "Stock is not available for medicine ID " + medicineId);
        return "redirect:/dashboard/pharmacist";
    }

    @PostMapping("/pharmacist/process-billing")
    public String processBilling(
            @RequestParam("orderId") Long orderId,
            RedirectAttributes redirectAttributes
    ) {
        Bill bill = pharmacistService.processCustomerBilling(orderId);
        redirectAttributes.addFlashAttribute("successMessage", "Billing completed. Bill ID: " + bill.getBillId());
        return "redirect:/dashboard/pharmacist";
    }

    @PostMapping("/pharmacist/update-inventory")
    public String updateInventory(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("qty") Integer qty,
            RedirectAttributes redirectAttributes
    ) {
        pharmacistService.updateInventoryStatus(medicineId, qty);
        redirectAttributes.addFlashAttribute("successMessage", "Inventory adjusted successfully.");
        return "redirect:/dashboard/pharmacist";
    }
}
