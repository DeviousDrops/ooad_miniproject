package com.pharmacy.service.actor;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.Shipment;
import com.pharmacy.repository.InvoiceRepository;
import com.pharmacy.repository.ShipmentRepository;
import com.pharmacy.service.inventory.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Service
@Transactional
public class SupplierService {

    private final InventoryService inventoryService;
    private final ShipmentRepository shipmentRepository;
    private final InvoiceRepository invoiceRepository;

    public SupplierService(
            InventoryService inventoryService,
            ShipmentRepository shipmentRepository,
            InvoiceRepository invoiceRepository
    ) {
        this.inventoryService = inventoryService;
        this.shipmentRepository = shipmentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public Shipment supplyRestock(
            Long supplierId,
            Long inventoryId,
            Long medicineId,
            Integer quantity,
            LocalDate expectedDate
    ) {
        return inventoryService.supplyRestock(
                new InventoryService.SupplyRestockCommand(
                        supplierId,
                        inventoryId,
                        medicineId,
                        quantity,
                        expectedDate
                )
        );
    }

    public Shipment shipmentVerification(Long shipmentId) {
        return inventoryService.shipmentVerification(shipmentId);
    }

    public Invoice submitDigitalInvoice(Long supplierId, Long shipmentId, String invoiceNumber, BigDecimal amount) {
        Long safeShipmentId = Objects.requireNonNull(shipmentId, "shipmentId is required");
        Shipment shipment = shipmentRepository.findById(safeShipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));

        if (!shipment.getSupplier().getUserId().equals(supplierId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier does not own this shipment");
        }

        Invoice invoice = new Invoice();
        invoice.setSupplier(shipment.getSupplier());
        invoice.setShipment(shipment);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setAmount(amount);
        return invoiceRepository.save(invoice);
    }
}
