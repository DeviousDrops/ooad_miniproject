package com.pharmacy.service;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceItem;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service("portalSupplierService")
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
    public Invoice generateSupplierBill(Long supplierId, List<Long> medicineIds, List<Integer> quantities, LocalDate expectedDate) {
        if (supplierId == null) {
            throw new IllegalArgumentException("Supplier ID is required");
        }
        if (medicineIds == null || quantities == null || medicineIds.isEmpty() || medicineIds.size() != quantities.size()) {
            throw new IllegalArgumentException("At least one medicine with quantity is required");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .or(() -> supplierRepository.findBySupplierId(supplierId))
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found for ID: " + supplierId));
        Inventory inventory = inventoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> inventoryRepository.save(new Inventory()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        Invoice invoice = billFactory.createInvoice(supplier, BigDecimal.ZERO);
        invoice.setPaymentStatus(Invoice.PaymentStatus.PENDING);
        invoice = invoiceRepository.save(invoice);

        for (int i = 0; i < medicineIds.size(); i++) {
            Long medicineId = medicineIds.get(i);
            Integer quantity = quantities.get(i);
            if (medicineId == null) {
                throw new IllegalArgumentException("Medicine selection is required for each bill line");
            }
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1 for each bill line");
            }

            Medicine medicine = medicineRepository.findById(medicineId)
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));

            InvoiceItem item = new InvoiceItem();
            item.setMedicine(medicine);
            item.setQuantity(quantity);
            item.setUnitPrice(medicine.getPrice());
            invoice.addItem(item);

            Shipment shipment = new Shipment();
            shipment.setSupplier(supplier);
            shipment.setInventory(inventory);
            shipment.setInvoice(invoice);
            shipment.setMedicine(medicine);
            shipment.setQuantity(quantity);
            shipment.setExpectedDate(expectedDate == null ? LocalDate.now().plusDays(2) : expectedDate);
            shipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT);
            shipmentRepository.save(shipment);

            totalAmount = totalAmount.add(
                    medicine.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP)
            );
        }

        invoice.setAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Shipment> listShipments() {
        return shipmentRepository.findAllByOrderByExpectedDateAsc();
    }

    @Transactional(readOnly = true)
    public List<Medicine> listMedicines() {
        return medicineRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Invoice> listInvoices() {
        return invoiceRepository.findAllByOrderBySubmittedAtDesc();
    }

    @Transactional(readOnly = true)
    public Long defaultSupplierId() {
        return supplierRepository.findBySupplierId(7001L)
                .map(Supplier::getSupplierId)
                .or(() -> supplierRepository.findFirstByOrderBySupplierIdAsc().map(Supplier::getSupplierId))
                .orElse(7001L);
    }

    @Transactional
    public Shipment updateShipmentStatus(Long shipmentId, Shipment.ShipmentStatus status) {
        if (shipmentId == null) {
            throw new IllegalArgumentException("Shipment ID is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Delivery status is required");
        }

        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment not found: " + shipmentId));

        if (shipment.getInvoice() != null && shipment.getInvoice().getPaymentStatus() != Invoice.PaymentStatus.PENDING) {
            throw new IllegalStateException("Only shipments for pending bills can be updated.");
        }

        Shipment.ShipmentStatus previousStatus = shipment.getStatus();
        if (previousStatus == Shipment.ShipmentStatus.DELIVERED && status == Shipment.ShipmentStatus.IN_TRANSIT) {
            throw new IllegalStateException("Delivered orders cannot be moved back to in transit.");
        }
        if (previousStatus == Shipment.ShipmentStatus.CANCELLED || previousStatus == Shipment.ShipmentStatus.DECLINED) {
            throw new IllegalStateException("Cancelled or declined orders cannot be updated.");
        }
        shipment.setStatus(status);
        if (status == Shipment.ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(java.time.LocalDateTime.now());
            if (previousStatus != Shipment.ShipmentStatus.DELIVERED) {
                adminService.updateMedicineInventory(shipment.getMedicine().getMedicineId(), shipment.getQuantity());
            }
        } else {
            shipment.setDeliveredAt(null);
        }
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Invoice cancelInvoice(Long invoiceId) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Bill ID is required");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + invoiceId));
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Paid bills cannot be cancelled.");
        }
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.DECLINED) {
            throw new IllegalStateException("Declined bills cannot be cancelled.");
        }
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Bill has already been cancelled.");
        }

        invoice.setPaymentStatus(Invoice.PaymentStatus.CANCELLED);
        invoice.setPaidAt(null);
        if (invoice.getShipments() != null) {
            invoice.getShipments().forEach(shipment -> {
                if (shipment.getStatus() != Shipment.ShipmentStatus.DELIVERED) {
                    shipment.setStatus(Shipment.ShipmentStatus.CANCELLED);
                    shipment.setDeliveredAt(null);
                    shipmentRepository.save(shipment);
                }
            });
        }
        return invoiceRepository.save(invoice);
    }
}
