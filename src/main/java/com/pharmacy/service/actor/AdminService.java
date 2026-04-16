package com.pharmacy.service.actor;

import com.pharmacy.domain.Bill;
import com.pharmacy.domain.Medicine;
import com.pharmacy.domain.Report;
import com.pharmacy.repository.BillRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ReportRepository;
import com.pharmacy.service.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AdminService {

    private final InventoryService inventoryService;
    private final BillRepository billRepository;
    private final MedicineRepository medicineRepository;
    private final ReportRepository reportRepository;

    public AdminService(
            InventoryService inventoryService,
            BillRepository billRepository,
            MedicineRepository medicineRepository,
            ReportRepository reportRepository
    ) {
        this.inventoryService = inventoryService;
        this.billRepository = billRepository;
        this.medicineRepository = medicineRepository;
        this.reportRepository = reportRepository;
    }

    public Medicine manageMedicineInventory(InventoryService.ManageMedicineCommand command) {
        return inventoryService.manageMedicineInventory(command);
    }

    public Report generateSalesAnalytics(String generatedBy) {
        List<Bill> bills = billRepository.findAll();
        BigDecimal totalSales = bills.stream()
                .map(Bill::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal avgBill = bills.isEmpty()
                ? BigDecimal.ZERO
                : totalSales.divide(BigDecimal.valueOf(bills.size()), 2, RoundingMode.HALF_UP);

        long lowStockCount = medicineRepository.findAll().stream().filter(Medicine::isLowStock).count();

        Report report = new Report();
        report.setReportType(Report.ReportType.SALES);
        report.setGeneratedBy(generatedBy);
        report.setSummary(
                "Total bills: " + bills.size() +
                        ", Total sales: " + totalSales +
                        ", Average bill: " + avgBill +
                        ", Low stock medicines: " + lowStockCount +
                        ", Date: " + LocalDate.now()
        );
        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<SupplyAutomationItem> automateMedicineSupply() {
        return medicineRepository.findAll().stream()
                .filter(Medicine::isLowStock)
                .map(medicine -> new SupplyAutomationItem(
                        medicine.getMedicineId(),
                        medicine.getName(),
                        medicine.getStockQty(),
                        medicine.getLowStockThreshold(),
                        Math.max(medicine.getLowStockThreshold() * 2, medicine.getLowStockThreshold() - medicine.getStockQty())
                ))
                .toList();
    }

    public record SupplyAutomationItem(
            Long medicineId,
            String medicineName,
            Integer currentStock,
            Integer threshold,
            Integer suggestedReorderQty
    ) {
    }
}
