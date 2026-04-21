package com.pharmacy.controller;

import com.pharmacy.model.Customer;
import com.pharmacy.repository.CustomerRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegistrationController {

    private final CustomerRepository customerRepository;
    private final UserDetailsService userDetailsService;

    public RegistrationController(CustomerRepository customerRepository, UserDetailsService userDetailsService) {
        this.customerRepository = customerRepository;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public String registerCustomer(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes
    ) {
        if (phone == null || !phone.matches("\\d{10}")) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phone number must be exactly 10 digits.");
            return "redirect:/login";
        }

        try {
            // 1. Add the new Customer to Spring Security's In-Memory Manager so they can immediately login
            if (userDetailsService instanceof InMemoryUserDetailsManager manager) {
                if (manager.userExists(phone)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Phone number is already registered.");
                    return "redirect:/login";
                }
                UserDetails newUser = User.withUsername(phone)
                        .password("{noop}" + password)
                        .roles("CUSTOMER")
                        .build();
                manager.createUser(newUser);
            }

            // 2. Save the entity to the persistent Database
            Customer customer = new Customer();
            customer.setName(fullName);
            customer.setPhone(phone);
            customer.setCustomerId(Long.parseLong(phone));
            customer.setEmail(phone + "@customer.pharmaflow.com"); // Auto-generating email since it is required and unique
            customer.setPassword(password);
            customerRepository.save(customer);

            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
        }
        return "redirect:/login";
    }
}