package com.pharmacy.controller;

import com.pharmacy.model.Customer;
import com.pharmacy.repository.CustomerRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final CustomerRepository customerRepository;

    public RegistrationController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @PostMapping("/register")
    public String registerCustomer(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes
    ) {
        String safeName = fullName == null ? "" : fullName.trim();
        String safePhone = phone == null ? "" : phone.trim();
        String safePassword = password == null ? "" : password;

        if (safeName.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Full name is required.");
            return "redirect:/login?tab=signup";
        }

        if (!safePhone.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phone number must be exactly 10 digits.");
            return "redirect:/login?tab=signup";
        }

        if (customerRepository.findFirstByNameIgnoreCase(safeName).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username is already taken. Please use a different full name.");
            return "redirect:/login?tab=signup";
        }

        if (safePassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be at least 6 characters.");
            return "redirect:/login?tab=signup";
        }

        if (customerRepository.findByPhone(safePhone).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phone number is already registered.");
            return "redirect:/login?tab=signup";
        }

        try {
            // Save persistent customer record first.
            Customer customer = new Customer();
                customer.setName(safeName);
                customer.setPhone(safePhone);
                customer.setCustomerId(Long.parseLong(safePhone));
                customer.setEmail(safePhone + "@customer.pharmaflow.com"); // Auto-generating email since it is required and unique
                customer.setPassword("{noop}" + safePassword);
            customerRepository.save(customer);

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "redirect:/login?tab=signup";
        }
        return "redirect:/login?tab=signup";
    }
}