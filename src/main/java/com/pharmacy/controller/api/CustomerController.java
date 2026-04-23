package com.pharmacy.controller.api;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Order;
import com.pharmacy.model.Prescription;
import com.pharmacy.service.actor.CustomerService;
import com.pharmacy.service.billing.BillingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/search")
    public List<Medicine> search(@RequestParam(name = "query", required = false) String query) {
        return customerService.searchMedicines(query);
    }

    @PostMapping("/place-order")
    public Order placeOrder(@Valid @RequestBody PlaceOrderRequest request, Authentication authentication) {
        List<BillingService.OrderLineCommand> lines = request.items().stream()
                .map(item -> new BillingService.OrderLineCommand(item.medicineId(), item.quantity()))
                .toList();
        return customerService.placeOrder(authentication.getName(), lines);
    }

    @GetMapping("/orders")
    public List<Order> orders(Authentication authentication) {
        return customerService.viewOrderHistory(authentication.getName());
    }

    @GetMapping("/prescriptions")
    public List<Prescription> prescriptions(Authentication authentication) {
        return customerService.viewPrescriptionHistory(authentication.getName());
    }

    @GetMapping("/bills")
    public List<Bill> billHistory(Authentication authentication) {
        return customerService.viewBillHistory(authentication.getName());
    }

    public record PlaceOrderRequest(
            @NotEmpty List<@Valid OrderLineItemRequest> items
    ) {
    }

    public record OrderLineItemRequest(@NotNull Long medicineId, @NotNull @Min(1) Integer quantity) {
    }
}
