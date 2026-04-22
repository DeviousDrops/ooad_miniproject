package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Inventory;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Report;
import com.pharmacy.model.Shipment;
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.repository.InvoiceRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ReportRepository;
import com.pharmacy.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service("portalAdminService")
@SuppressWarnings("null")
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final BillRepository billRepository;
    private final InvoiceRepository invoiceRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReportRepository reportRepository;
    private final InventoryObserver inventoryObserver;

    private final List<String> automatedActions = new ArrayList<>();

    public AdminService(
            MedicineRepository medicineRepository,
            InventoryRepository inventoryRepository,
            BillRepository billRepository,
            InvoiceRepository invoiceRepository,
            ShipmentRepository shipmentRepository,
            ReportRepository reportRepository,
            InventoryObserver inventoryObserver
    ) {
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
        this.shipmentRepository = shipmentRepository;
        this.reportRepository = reportRepository;
        this.inventoryObserver = inventoryObserver;
    }

    @Transactional
    public Medicine manageMedicineInventory(Medicine medicine, Long inventoryId) {
        Inventory inventory = resolveInventory(inventoryId);
        Medicine target = medicine.getMedicineId() == null
                ? medicine
                : medicineRepository.findById(medicine.getMedicineId())
                        .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicine.getMedicineId()));

        Inventory previousInventory = target.getInventory();
        target.setName(medicine.getName());
        target.setCategory(medicine.getCategory());
        target.setManufacturer(medicine.getManufacturer());
        target.setPrice(medicine.getPrice());
        target.setStockQty(medicine.getStockQty());
        target.setExpiryDate(medicine.getExpiryDate());
        target.setLowStockThreshold(medicine.getLowStockThreshold());
        if (medicine.getMedicineType() != null) {
            target.setMedicineType(medicine.getMedicineType());
        } else if (target.getMedicineType() == null) {
            target.setMedicineType(Medicine.MedicineType.OTHER);
        }
        target.setInventory(inventory);

        if (target.getLowStockThreshold() == null || target.getLowStockThreshold() < 1) {
            target.setLowStockThreshold(InventoryObserver.LOW_STOCK_THRESHOLD);
        }
        if (target.getStockQty() == null) {
            target.setStockQty(0);
        }

        Medicine saved = medicineRepository.save(target);
        syncInventory(previousInventory);
        syncInventory(inventory);
        inventoryObserver.checkLowStock(saved);
        return saved;
    }

    @Transactional
    public void deleteMedicine(Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));
        Inventory inventory = medicine.getInventory();
        medicineRepository.delete(medicine);
        syncInventory(inventory);
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
    public List<Shipment> listShipments() {
        return shipmentRepository.findAllByOrderByExpectedDateAsc();
    }

    @Transactional(readOnly = true)
    public List<String> currentLowStockAlerts() {
        return medicineRepository.findAll().stream()
                .filter(medicine -> medicine.getStockQty() != null && medicine.getStockQty() <= InventoryObserver.LOW_STOCK_THRESHOLD)
                .map(medicine -> "Low stock alert for " + medicine.getName() + " (qty=" + medicine.getStockQty() + ")")
                .toList();
    }

    @Transactional
    public Report generateInventoryReport() {
        List<Medicine> medicines = medicineRepository.findAll();
        long lowStock = medicines.stream().filter(medicine -> medicine.getStockQty() <= InventoryObserver.LOW_STOCK_THRESHOLD).count();

        Report report = new Report();
        report.setReportType(Report.ReportType.INVENTORY);
        report.setGeneratedBy("ADMIN");
        report.setSummary("Inventory report on " + LocalDate.now() + " | total medicines=" + medicines.size() + " | low stock=" + lowStock);
        report.setData(report.getSummary());
        return reportRepository.save(report);
    }

    @Transactional
    public Report generateSalesAnalytics() {
        List<String> rows = fetchSalesData();

        BigDecimal total = billRepository.findAll().stream()
                .map(Bill::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        Report report = new Report();
        report.setReportType(Report.ReportType.SALES);
        report.setGeneratedBy("ADMIN");
        report.setSummary("Sales analytics on " + LocalDate.now() + " | records=" + rows.size() + " | total sales=" + total);
        report.setData(String.join("\n", rows));
        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<String> fetchSalesData() {
        return billRepository.findAll().stream()
                .map(bill -> "billId=" + bill.getBillId() + ", total=" + bill.getTotal() + ", generatedAt=" + bill.getGeneratedAt())
                .toList();
    }

    @Transactional
    public void updateMedicineInventory(Long medicineId, int qtyDelta) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));

        if (qtyDelta >= 0) {
            medicine.increaseStock(qtyDelta);
        } else {
            medicine.reduceStock(Math.abs(qtyDelta));
        }
        medicineRepository.save(medicine);
        inventoryObserver.checkLowStock(medicine);
    }

    @Transactional
    public Invoice payInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + invoiceId));
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Bill has already been processed.");
        }
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.CANCELLED
                || invoice.getPaymentStatus() == Invoice.PaymentStatus.DECLINED) {
            throw new IllegalStateException("Cancelled or declined bills cannot be paid.");
        }
        invoice.setPaymentStatus(Invoice.PaymentStatus.PROCESSED);
        invoice.setPaidAt(java.time.LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice declineInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + invoiceId));
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.PROCESSED) {
            throw new IllegalStateException("Paid bills cannot be declined.");
        }
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled bills cannot be declined.");
        }
        if (invoice.getPaymentStatus() == Invoice.PaymentStatus.DECLINED) {
            throw new IllegalStateException("Bill has already been declined.");
        }

        invoice.setPaymentStatus(Invoice.PaymentStatus.DECLINED);
        invoice.setPaidAt(null);
        if (invoice.getShipments() != null) {
            invoice.getShipments().forEach(shipment -> {
                if (shipment.getStatus() != Shipment.ShipmentStatus.DELIVERED) {
                    shipment.setStatus(Shipment.ShipmentStatus.DECLINED);
                    shipment.setDeliveredAt(null);
                    shipmentRepository.save(shipment);
                }
            });
        }
        return invoiceRepository.save(invoice);
    }

    // Scheduling requirement: periodic automation reads observer alerts and proposes replenishment actions.
    @Scheduled(fixedDelay = 60000)
    @Transactional(readOnly = true)
    public void automateMedicineSupply() {
        List<String> alerts = inventoryObserver.latestAlerts();
        if (alerts.isEmpty()) {
            return;
        }
        String action = "Auto-restock run at " + LocalDate.now() + " using " + alerts.size() + " low-stock alert(s)";
        synchronized (automatedActions) {
            automatedActions.add(0, action);
            if (automatedActions.size() > 100) {
                automatedActions.remove(automatedActions.size() - 1);
            }
        }
        log.info(action);
    }

    @Transactional(readOnly = true)
    public List<String> latestAutomationActions() {
        synchronized (automatedActions) {
            return List.copyOf(automatedActions);
        }
    }

    private Inventory resolveInventory(Long inventoryId) {
        if (inventoryId != null) {
            return inventoryRepository.findById(inventoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Inventory not found: " + inventoryId));
        }
        return inventoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> inventoryRepository.save(new Inventory()));
    }

    private void syncInventory(Inventory inventory) {
        if (inventory == null || inventory.getInventoryId() == null) {
            return;
        }
        int totalQuantity = medicineRepository.findByInventory_InventoryId(inventory.getInventoryId()).stream()
                .map(Medicine::getStockQty)
                .filter(qty -> qty != null)
                .mapToInt(Integer::intValue)
                .sum();
        inventory.setQuantity(totalQuantity);
        inventory.touch();
        inventoryRepository.save(inventory);
    }
}
