package com.pharmacy.controller;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Order;
import com.pharmacy.model.Payment;
import com.pharmacy.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
            Authentication authentication,
            Model model
    ) {
        String customerPhone = currentCustomerPhone(authentication);

        List<Order> orders = customerService.viewOrderHistory(customerPhone);
        List<Bill> bills = customerService.viewBillHistory(customerPhone);
        List<com.pharmacy.model.Prescription> prescriptions = customerService.viewPrescriptionHistory(customerPhone);

        model.addAttribute("medicines", customerService.searchMedicines(query));
        model.addAttribute("orders", orders);
        model.addAttribute("bills", bills);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("customerPhone", customerPhone);
        model.addAttribute("query", query == null ? "" : query);
        return "dashboard/customer";
    }

    @PostMapping("/customer/order")
    public String placeOrder(
            @RequestParam("medicineId") Long medicineId,
            @RequestParam("quantity") Integer quantity,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String customerPhone = currentCustomerPhone(authentication);
        try {
            Order placedOrder = customerService.placeOrder(
                    customerPhone,
                    List.of(new CustomerService.OrderRequestItem(medicineId, quantity))
            );
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully and stock verified.");
            redirectAttributes.addFlashAttribute("latestOrderId", placedOrder.getOrderId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("infoMessage", ex.getMessage());
        }
        return "redirect:/dashboard/customer";
    }

    @PostMapping("/customer/payment")
    public String makePayment(
            @RequestParam("billId") Long billId,
            @RequestParam("paymentMethod") Payment.PaymentMethod paymentMethod,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        customerService.makePayment(billId, paymentMethod);
        redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully.");
        return "redirect:/dashboard/customer";
    }

    @PostMapping("/customer/bill-history")
    public String showBills(Authentication authentication, RedirectAttributes redirectAttributes) {
        String customerPhone = currentCustomerPhone(authentication);
        List<Bill> bills = customerService.viewBillHistory(customerPhone);
        redirectAttributes.addFlashAttribute("infoMessage", "Loaded " + bills.size() + " bill record(s).");
        return "redirect:/dashboard/customer";
    }

    private String currentCustomerPhone(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Customer session is missing");
        }
        return authentication.getName();
    }
}
