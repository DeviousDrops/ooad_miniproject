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

import java.time.LocalDate;
import java.util.List;

@Controller("webSupplierController")
@PreAuthorize("hasRole('SUPPLIER')")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/dashboard/supplier")
    public String supplierDashboard(Model model) {
        model.addAttribute("medicines", supplierService.listMedicines());
        model.addAttribute("shipments", supplierService.listShipments());
        model.addAttribute("invoices", supplierService.listInvoices());
        model.addAttribute("defaultSupplierId", supplierService.defaultSupplierId());
        return "dashboard/supplier";
    }

    @PostMapping("/supplier/invoice")
    public String submitInvoice(
            @RequestParam("supplierId") Long supplierId,
            @RequestParam("medicineId") List<Long> medicineIds,
            @RequestParam("quantity") List<Integer> quantities,
            @RequestParam("expectedDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDate,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Invoice invoice = supplierService.generateSupplierBill(supplierId, medicineIds, quantities, expectedDate);
            redirectAttributes.addFlashAttribute("successMessage", "Bill generated successfully. ID: " + invoice.getInvoiceId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to generate bill. Please verify medicine selections and quantities.");
        }
        return "redirect:/dashboard/supplier";
    }

    @PostMapping("/supplier/shipments/status")
    public String updateShipmentStatus(
            @RequestParam("shipmentId") Long shipmentId,
            @RequestParam("status") Shipment.ShipmentStatus status,
            RedirectAttributes redirectAttributes
    ) {
        try {
            supplierService.updateShipmentStatus(shipmentId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Delivery status updated successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to update shipment status. Please verify shipment ID and status.");
        }
        return "redirect:/dashboard/supplier";
    }

    @PostMapping("/supplier/invoice/cancel")
    public String cancelInvoice(
            @RequestParam("invoiceId") Long invoiceId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            supplierService.cancelInvoice(invoiceId);
            redirectAttributes.addFlashAttribute("successMessage", "Bill cancelled successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to cancel bill. Please verify bill ID.");
        }
        return "redirect:/dashboard/supplier";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleSupplierFlowErrors(RuntimeException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/dashboard/supplier";
    }
}
