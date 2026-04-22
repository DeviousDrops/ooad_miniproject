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
            @RequestParam(name = "searchBy", required = false, defaultValue = "NAME") String searchBy,
            @RequestParam(name = "selectedValue", required = false) String selectedValue,
            Authentication authentication,
            Model model
    ) {
        String customerPhone = currentCustomerPhone(authentication);

        List<Order> orders = customerService.viewOrderHistory(customerPhone);
        List<Bill> bills = customerService.viewBillHistory(customerPhone);

        model.addAttribute("medicines", customerService.searchMedicines(searchBy, selectedValue));
        model.addAttribute("medicineNames", customerService.availableMedicineNames());
        model.addAttribute("medicineCategories", customerService.availableMedicineCategories());
        model.addAttribute("orders", orders);
        model.addAttribute("bills", bills);
        model.addAttribute("customerPhone", customerPhone);
        model.addAttribute("searchBy", searchBy);
        model.addAttribute("selectedValue", selectedValue == null ? "" : selectedValue);
        return "dashboard/customer";
    }

    @PostMapping("/customer/order")
    public String placeOrder(
            @RequestParam("medicineId") List<Long> medicineIds,
            @RequestParam("quantity") List<Integer> quantities,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String customerPhone = currentCustomerPhone(authentication);
        try {
            if (medicineIds.size() != quantities.size()) {
                throw new IllegalArgumentException("Each selected medicine must have a quantity.");
            }

            List<CustomerService.OrderRequestItem> items = java.util.stream.IntStream.range(0, medicineIds.size())
                    .mapToObj(index -> new CustomerService.OrderRequestItem(medicineIds.get(index), quantities.get(index)))
                    .toList();

            Order placedOrder = customerService.placeOrder(
                    customerPhone,
                    items
            );
            redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully and stock verified.");
            redirectAttributes.addFlashAttribute("latestOrderId", placedOrder.getOrderId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to place order. Please verify medicine ID and quantity values.");
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
        try {
            customerService.makePayment(billId, paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "Payment processed successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Unable to process payment. Please verify bill ID and try again.");
        }
        return "redirect:/dashboard/customer";
    }

    @PostMapping("/customer/order/cancel")
    public String cancelOrder(
            @RequestParam("orderId") Long orderId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String customerPhone = currentCustomerPhone(authentication);
        try {
            customerService.cancelOrder(customerPhone, orderId);
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/customer";
    }

    @PostMapping("/customer/bill-history")
    public String showBills(Authentication authentication, RedirectAttributes redirectAttributes) {
        String customerPhone = currentCustomerPhone(authentication);
        try {
            List<Bill> bills = customerService.viewBillHistory(customerPhone);
            redirectAttributes.addFlashAttribute("infoMessage", "Loaded " + bills.size() + " bill record(s).");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard/customer";
    }

    private String currentCustomerPhone(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Customer session is missing");
        }
        return authentication.getName();
    }
}
