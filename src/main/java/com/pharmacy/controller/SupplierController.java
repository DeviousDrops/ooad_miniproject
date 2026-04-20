package com.pharmacy.controller;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.Shipment;
import com.pharmacy.service.SupplierService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller("webSupplierController")
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/dashboard/supplier")
    public String supplierDashboard(Model model) {
        model.addAttribute("shipments", supplierService.listShipments());
        return "dashboard/supplier";
    }

    @PostMapping("/supplier/restock")
    public String supplyRestock(
            @RequestParam("supplierId") Long supplierId,
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("expectedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDate,
            RedirectAttributes redirectAttributes
    ) {
        Shipment shipment = supplierService.supplyRestock(supplierId, medicineId, quantity, expectedDate);
        redirectAttributes.addFlashAttribute("successMessage", "Restock shipment created. ID: " + shipment.getShipmentId());
        return "redirect:/dashboard/supplier";
    }

    @PostMapping("/supplier/verify-shipment")
    public String verifyShipment(
            @RequestParam("shipmentId") Long shipmentId,
            RedirectAttributes redirectAttributes
    ) {
        boolean verified = supplierService.shipmentVerification(shipmentId);
        redirectAttributes.addFlashAttribute("infoMessage", verified
                ? "Shipment verified successfully."
                : "Shipment verification failed.");
        return "redirect:/dashboard/supplier";
    }

    @PostMapping("/supplier/invoice")
    public String submitInvoice(
            @RequestParam("shipmentId") Long shipmentId,
            @RequestParam("amount") BigDecimal amount,
            RedirectAttributes redirectAttributes
    ) {
        Invoice invoice = supplierService.submitDigitalInvoice(shipmentId, amount);
        redirectAttributes.addFlashAttribute("successMessage", "Invoice submitted. ID: " + invoice.getInvoiceId());
        return "redirect:/dashboard/supplier";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleSupplierFlowErrors(RuntimeException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("infoMessage", ex.getMessage());
        return "redirect:/dashboard/supplier";
    }
}
