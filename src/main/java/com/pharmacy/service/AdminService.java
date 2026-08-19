package com.pharmacy.service;

import com.pharmacy.model.Bill;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Report;
import com.pharmacy.model.Shipment;
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.InvoiceRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ReportRepository;
import com.pharmacy.repository.ShipmentRepository;
import com.pharmacy.pattern.factory.MedicineFactorySelector;
import com.pharmacy.pattern.observer.AdminAlertObserver;
import com.pharmacy.pattern.observer.InventoryAlertSubject;
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
    private final BillRepository billRepository;
    private final InvoiceRepository invoiceRepository;
    private final ShipmentRepository shipmentRepository;
    private final ReportRepository reportRepository;
    private final InventoryAlertSubject inventoryAlertSubject;
    private final AdminAlertObserver adminAlertObserver;
    private final MedicineFactorySelector medicineFactorySelector;

    private final List<String> automatedActions = new ArrayList<>();

    public AdminService(
            MedicineRepository medicineRepository,
            BillRepository billRepository,
            InvoiceRepository invoiceRepository,
            ShipmentRepository shipmentRepository,
            ReportRepository reportRepository,
            InventoryAlertSubject inventoryAlertSubject,
            AdminAlertObserver adminAlertObserver,
            MedicineFactorySelector medicineFactorySelector
    ) {
        this.medicineRepository = medicineRepository;
        this.billRepository = billRepository;
        this.invoiceRepository = invoiceRepository;
        this.shipmentRepository = shipmentRepository;
        this.reportRepository = reportRepository;
        this.inventoryAlertSubject = inventoryAlertSubject;
        this.adminAlertObserver = adminAlertObserver;
        this.medicineFactorySelector = medicineFactorySelector;
    }

    // Factory Method: the concrete Medicine subclass is resolved once, at creation, and never changes.
    @Transactional
    public Medicine createMedicine(
            String medicineType,
            String name,
            String category,
            String manufacturer,
            java.math.BigDecimal price,
            Integer stockQty,
            java.time.LocalDate expiryDate,
            Integer lowStockThreshold
    ) {
        Medicine medicine = medicineFactorySelector.byType(medicineType)
                .createMedicine(name, category, price, stockQty, expiryDate, lowStockThreshold);
        medicine.setManufacturer(manufacturer);

        Medicine saved = medicineRepository.save(medicine);
        inventoryAlertSubject.notifyLowStockOrExpiry(saved);
        return saved;
    }

    @Transactional
    public Medicine updateMedicine(
            Long medicineId,
            String name,
            String category,
            String manufacturer,
            java.math.BigDecimal price,
            Integer stockQty,
            java.time.LocalDate expiryDate,
            Integer lowStockThreshold
    ) {
        Medicine target = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));

        target.setName(name);
        target.setCategory(category);
        target.setManufacturer(manufacturer);
        target.setPrice(price);
        target.setStockQty(stockQty == null ? 0 : stockQty);
        target.setExpiryDate(expiryDate);
        target.setLowStockThreshold(lowStockThreshold == null || lowStockThreshold < 1 ? 10 : lowStockThreshold);

        Medicine saved = medicineRepository.save(target);
        inventoryAlertSubject.notifyLowStockOrExpiry(saved);
        return saved;
    }

    @Transactional
    public void deleteMedicine(Long medicineId) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));
        medicineRepository.delete(medicine);
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
        return adminAlertObserver.latestAlerts();
    }

    @Transactional
    public Report generateInventoryReport() {
        List<Medicine> medicines = medicineRepository.findAll();
        long lowStock = medicines.stream().filter(Medicine::isLowStock).count();

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
        inventoryAlertSubject.notifyLowStockOrExpiry(medicine);
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
        List<String> alerts = adminAlertObserver.latestAlerts();
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

}
