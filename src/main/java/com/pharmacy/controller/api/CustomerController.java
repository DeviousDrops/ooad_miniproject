package com.pharmacy.controller.api;

import com.pharmacy.domain.Bill;
import com.pharmacy.domain.Medicine;
import com.pharmacy.domain.Order;
import com.pharmacy.domain.Prescription;
import com.pharmacy.service.actor.CustomerService;
import com.pharmacy.service.billing.BillingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    public Order placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        List<BillingService.OrderLineCommand> lines = request.items().stream()
                .map(item -> new BillingService.OrderLineCommand(item.medicineId(), item.quantity()))
                .toList();
        return customerService.placeOrder(request.customerId(), lines);
    }

    @GetMapping("/prescriptions/{customerId}")
    public List<Prescription> prescriptions(@PathVariable("customerId") Long customerId) {
        return customerService.viewPrescriptionHistory(customerId);
    }

    @GetMapping("/bills/{customerId}")
    public List<Bill> billHistory(@PathVariable("customerId") Long customerId) {
        return customerService.viewBillHistory(customerId);
    }

    public record PlaceOrderRequest(
            @NotNull Long customerId,
            @NotEmpty List<@Valid OrderLineItemRequest> items
    ) {
    }

    public record OrderLineItemRequest(@NotNull Long medicineId, @NotNull @Min(1) Integer quantity) {
    }
}
