package com.pharmacy.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RouteController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard() {
        return "forward:/dashboard/admin.html";
    }

    @GetMapping("/dashboard/pharmacist")
    public String pharmacistDashboard() {
        return "forward:/dashboard/pharmacist.html";
    }

    @GetMapping("/dashboard/customer")
    public String customerDashboard() {
        return "forward:/dashboard/customer.html";
    }

    @GetMapping("/dashboard/supplier")
    public String supplierDashboard() {
        return "forward:/dashboard/supplier.html";
    }
}
