package com.pharmacy.service.actor;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceItem;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Shipment;
import com.pharmacy.repository.InvoiceRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.SupplierRepository;
import com.pharmacy.service.BillFactory;
import com.pharmacy.service.inventory.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SupplierService {

    private final InventoryService inventoryService;
    private final InvoiceRepository invoiceRepository;
    private final MedicineRepository medicineRepository;
    private final SupplierRepository supplierRepository;
    private final BillFactory billFactory;

    public SupplierService(
            InventoryService inventoryService,
            InvoiceRepository invoiceRepository,
            MedicineRepository medicineRepository,
            SupplierRepository supplierRepository,
            BillFactory billFactory
    ) {
        this.inventoryService = inventoryService;
        this.invoiceRepository = invoiceRepository;
        this.medicineRepository = medicineRepository;
        this.supplierRepository = supplierRepository;
        this.billFactory = billFactory;
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

    public Invoice generateSupplierBill(Long supplierId, List<Long> medicineIds, List<Integer> quantities) {
        if (supplierId == null) {
            throw new IllegalArgumentException("supplierId is required");
        }
        if (medicineIds == null || quantities == null || medicineIds.isEmpty() || medicineIds.size() != quantities.size()) {
            throw new IllegalArgumentException("At least one medicine line is required");
        }

        var supplier = supplierRepository.findBySupplierId(supplierId)
                .or(() -> supplierRepository.findFirstByOrderBySupplierIdAsc())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        Invoice invoice = billFactory.createInvoice(supplier, BigDecimal.ZERO);
        invoice.setPaymentStatus(Invoice.PaymentStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < medicineIds.size(); i++) {
            Long medicineId = medicineIds.get(i);
            if (medicineId == null) {
                throw new IllegalArgumentException("Medicine id is required");
            }

            Medicine medicine = medicineRepository.findById(medicineId)
                    .orElseThrow(() -> new IllegalArgumentException("Medicine not found: " + medicineId));
            Integer quantity = quantities.get(i);
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }

            InvoiceItem item = new InvoiceItem();
            item.setMedicine(medicine);
            item.setQuantity(quantity);
            item.setUnitPrice(medicine.getPrice());
            invoice.addItem(item);

            total = total.add(medicine.getPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP));
        }

        invoice.setAmount(total.setScale(2, RoundingMode.HALF_UP));
        return invoiceRepository.save(invoice);
    }
}
