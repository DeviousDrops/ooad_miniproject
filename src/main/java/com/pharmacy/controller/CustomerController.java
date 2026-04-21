package com.pharmacy.controller;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Payment;
import com.pharmacy.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller("webCustomerController")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/dashboard/customer")
    public String customerDashboard(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "customerId", required = false) Long customerId,
            Model model
    ) {
        Long effectiveCustomerId = customerId == null ? customerService.defaultCustomerReference() : customerId;

        List<Bill> bills;
        List<com.pharmacy.model.Prescription> prescriptions;
        try {
            bills = customerService.viewBillHistory(effectiveCustomerId);
            prescriptions = customerService.viewPrescriptionHistory(effectiveCustomerId);
        } catch (IllegalArgumentException ex) {
            effectiveCustomerId = customerService.defaultCustomerReference();
            bills = customerService.viewBillHistory(effectiveCustomerId);
            prescriptions = customerService.viewPrescriptionHistory(effectiveCustomerId);
            if (!model.containsAttribute("infoMessage")) {
                model.addAttribute("infoMessage", ex.getMessage());
            }
        }

        model.addAttribute("medicines", customerService.searchMedicines(query));
        model.addAttribute("bills", bills);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("customerId", effectiveCustomerId);
        model.addAttribute("query", query == null ? "" : query);
        return "dashboard/customer";
    }

    @PostMapping("/customer/order")
    public String placeOrder(
            @RequestParam("customerId") Long customerId,
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("quantity") Integer quantity,
            RedirectAttributes redirectAttributes
    ) {
        try {
            com.pharmacy.model.Order placedOrder = customerService.placeOrder(
                    customerId,
                    List.of(new CustomerService.OrderRequestItem(medicineId, quantity))
            );
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully and stock verified.");
            redirectAttributes.addFlashAttribute("latestOrderId", placedOrder.getOrderId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("infoMessage", ex.getMessage());
        }
        return "redirect:/dashboard/customer?customerId=" + customerId;
    }

    @PostMapping("/customer/payment")
    public String makePayment(
            @RequestParam("customerId") Long customerId,
            @RequestParam("billId") Long billId,
            @RequestParam("paymentMethod") Payment.PaymentMethod paymentMethod,
            RedirectAttributes redirectAttributes
    ) {
        customerService.makePayment(billId, paymentMethod);
        redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully.");
        return "redirect:/dashboard/customer?customerId=" + customerId;
    }

    @PostMapping("/customer/bill-history")
    public String showBills(
            @RequestParam("customerId") Long customerId,
            RedirectAttributes redirectAttributes
    ) {
        List<Bill> bills = customerService.viewBillHistory(customerId);
        redirectAttributes.addFlashAttribute("infoMessage", "Loaded " + bills.size() + " bill record(s).");
        return "redirect:/dashboard/customer?customerId=" + customerId;
    }
}
