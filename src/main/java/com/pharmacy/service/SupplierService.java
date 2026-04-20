package com.pharmacy.service;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.Inventory;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Shipment;
import com.pharmacy.model.Supplier;
import com.pharmacy.repository.InvoiceRepository;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ShipmentRepository;
import com.pharmacy.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service("portalSupplierService")
@SuppressWarnings("null")
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ShipmentRepository shipmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final BillFactory billFactory;
    private final AdminService adminService;

    public SupplierService(
            SupplierRepository supplierRepository,
            ShipmentRepository shipmentRepository,
            InvoiceRepository invoiceRepository,
            MedicineRepository medicineRepository,
            InventoryRepository inventoryRepository,
            BillFactory billFactory,
            AdminService adminService
    ) {
        this.supplierRepository = supplierRepository;
        this.shipmentRepository = shipmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.billFactory = billFactory;
        this.adminService = adminService;
    }

    @Transactional
    public Shipment supplyRestock(Long supplierId, Long medicineId, Integer qty, LocalDate expectedDate) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required");
        }
        if (medicineId == null) {
            throw new IllegalArgumentException("Medicine ID is required");
        }
        if (qty == null || qty < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .or(() -> supplierRepository.findBySupplierId(supplierId))
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found for ID: " + supplierId));

        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));

        Inventory inventory = inventoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> inventoryRepository.save(new Inventory()));

        Shipment shipment = new Shipment();
        shipment.setSupplier(supplier);
        shipment.setMedicine(medicine);
        shipment.setInventory(inventory);
        shipment.setQuantity(qty);
        shipment.setExpectedDate(expectedDate == null ? LocalDate.now().plusDays(2) : expectedDate);
        shipment.setStatus(Shipment.ShipmentStatus.PENDING);
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public boolean shipmentVerification(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        boolean verified = shipment.verifyShipment();
        shipmentRepository.save(shipment);
        return verified;
    }

    @Transactional
    public Invoice submitDigitalInvoice(Long shipmentId, BigDecimal amount) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        if (shipment.getStatus() != Shipment.ShipmentStatus.VERIFIED) {
            throw new IllegalStateException("Shipment must be verified before invoice submission");
        }

        BigDecimal finalAmount = amount == null ? BigDecimal.ZERO : amount;
        Invoice invoice = billFactory.createInvoice(shipment.getSupplier(), shipment, finalAmount);
        Invoice saved = invoiceRepository.save(invoice);

        // Include relation from supplier flow to admin inventory update use case.
        adminService.updateMedicineInventory(shipment.getMedicine().getMedicineId(), shipment.getQuantity());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Shipment> listShipments() {
        return shipmentRepository.findAll();
    }
}
