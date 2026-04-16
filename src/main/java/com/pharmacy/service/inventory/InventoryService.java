package com.pharmacy.service.inventory;

import com.pharmacy.domain.Inventory;
import com.pharmacy.domain.Medicine;
import com.pharmacy.domain.Shipment;
import com.pharmacy.domain.Supplier;
import com.pharmacy.pattern.factory.MedicineFactory;
import com.pharmacy.pattern.factory.MedicineFactorySelector;
import com.pharmacy.pattern.observer.AdminAlertObserver;
import com.pharmacy.pattern.observer.InventoryAlertSubject;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.ShipmentRepository;
import com.pharmacy.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class InventoryService {

    private final MedicineRepository medicineRepository;
    private final InventoryRepository inventoryRepository;
    private final ShipmentRepository shipmentRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineFactorySelector medicineFactorySelector;
    private final InventoryAlertSubject inventoryAlertSubject;
    private final AdminAlertObserver adminAlertObserver;

    public InventoryService(
            MedicineRepository medicineRepository,
            InventoryRepository inventoryRepository,
            ShipmentRepository shipmentRepository,
            SupplierRepository supplierRepository,
            MedicineFactorySelector medicineFactorySelector,
            InventoryAlertSubject inventoryAlertSubject,
            AdminAlertObserver adminAlertObserver
    ) {
        this.medicineRepository = medicineRepository;
        this.inventoryRepository = inventoryRepository;
        this.shipmentRepository = shipmentRepository;
        this.supplierRepository = supplierRepository;
        this.medicineFactorySelector = medicineFactorySelector;
        this.inventoryAlertSubject = inventoryAlertSubject;
        this.adminAlertObserver = adminAlertObserver;
    }

    public Medicine manageMedicineInventory(ManageMedicineCommand command) {
        Long inventoryId = Objects.requireNonNull(command.inventoryId(), "inventoryId is required");
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));

        Medicine medicine;
        if (command.medicineId() != null) {
            medicine = getMedicine(command.medicineId());
            medicine.setName(command.name());
            medicine.setCategory(command.category());
            medicine.setPrice(command.price());
            medicine.setExpiryDate(command.expiryDate());
            medicine.setLowStockThreshold(command.lowStockThreshold());
            medicine.setMedicineType(parseType(command.medicineType()));
            if (command.stockQty() != null) {
                medicine.setStockQty(command.stockQty());
            }
        } else {
            MedicineFactory factory = medicineFactorySelector.byType(command.medicineType());
            medicine = factory.createMedicine(
                    command.name(),
                    command.category(),
                    command.price(),
                    command.stockQty(),
                    command.expiryDate(),
                    command.lowStockThreshold()
            );
            inventory.addMedicine(medicine);
        }

        Medicine saved = medicineRepository.save(Objects.requireNonNull(medicine, "medicine is required"));
        inventory.touch();
        inventoryRepository.save(inventory);
        inventoryAlertSubject.notifyLowStockOrExpiry(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Medicine> listMedicines() {
        return medicineRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Medicine> searchMedicines(String query) {
        if (query == null || query.isBlank()) {
            return medicineRepository.findAll();
        }
        return medicineRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(query, query);
    }

    @Transactional(readOnly = true)
    public boolean verifyStock(Long medicineId, Integer requiredQty) {
        Medicine medicine = getMedicine(medicineId);
        return medicine.getStockQty() >= requiredQty;
    }

    public Inventory updateInventoryStatus(Long inventoryId) {
        Long safeInventoryId = Objects.requireNonNull(inventoryId, "inventoryId is required");
        Inventory inventory = inventoryRepository.findById(safeInventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
        inventory.touch();
        return inventoryRepository.save(inventory);
    }

    public Shipment supplyRestock(SupplyRestockCommand command) {
        Long supplierId = Objects.requireNonNull(command.supplierId(), "supplierId is required");
        Long inventoryId = Objects.requireNonNull(command.inventoryId(), "inventoryId is required");
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found"));
        Medicine medicine = getMedicine(command.medicineId());

        Shipment shipment = new Shipment();
        shipment.setSupplier(supplier);
        shipment.setInventory(inventory);
        shipment.setMedicine(medicine);
        shipment.setQuantity(command.quantity());
        shipment.setExpectedDate(command.expectedDate());
        shipment.setStatus(Shipment.ShipmentStatus.PENDING);

        return shipmentRepository.save(shipment);
    }

    public Shipment shipmentVerification(Long shipmentId) {
        Long safeShipmentId = Objects.requireNonNull(shipmentId, "shipmentId is required");
        Shipment shipment = shipmentRepository.findById(safeShipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipment not found"));

        if (shipment.getStatus() == Shipment.ShipmentStatus.VERIFIED) {
            return shipment;
        }

        shipment.setStatus(Shipment.ShipmentStatus.VERIFIED);
        shipment.setVerifiedAt(LocalDateTime.now());

        Medicine medicine = shipment.getMedicine();
        medicine.increaseStock(shipment.getQuantity());
        medicineRepository.save(medicine);

        Inventory inventory = shipment.getInventory();
        inventory.touch();
        inventoryRepository.save(inventory);

        inventoryAlertSubject.notifyLowStockOrExpiry(medicine);
        return shipmentRepository.save(shipment);
    }

    public Map<Long, Boolean> verifyBulkStock(Map<Long, Integer> quantityByMedicine) {
        Map<Long, Boolean> verification = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : quantityByMedicine.entrySet()) {
            verification.put(entry.getKey(), verifyStock(entry.getKey(), entry.getValue()));
        }
        return verification;
    }

    public void deductStockForBilling(Map<Long, Integer> quantityByMedicine) {
        for (Map.Entry<Long, Integer> entry : quantityByMedicine.entrySet()) {
            Medicine medicine = getMedicine(entry.getKey());
            int required = entry.getValue();
            if (medicine.getStockQty() < required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for medicine " + medicine.getName());
            }
            medicine.reduceStock(required);
            medicineRepository.save(medicine);
            inventoryAlertSubject.notifyLowStockOrExpiry(medicine);
        }
    }

    @Transactional(readOnly = true)
    public List<String> latestAlerts() {
        return adminAlertObserver.latestAlerts();
    }

    @Transactional(readOnly = true)
    public Medicine getMedicine(Long medicineId) {
        Long safeMedicineId = Objects.requireNonNull(medicineId, "medicineId is required");
        return medicineRepository.findById(safeMedicineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));
    }

    @Transactional(readOnly = true)
    public Inventory getDefaultInventory() {
        return inventoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> inventoryRepository.save(new Inventory()));
    }

    private Medicine.MedicineType parseType(String type) {
        if (type == null || type.isBlank()) {
            return Medicine.MedicineType.OTHER;
        }
        return switch (type.trim().toUpperCase()) {
            case "TABLET" -> Medicine.MedicineType.TABLET;
            case "SYRUP" -> Medicine.MedicineType.SYRUP;
            default -> Medicine.MedicineType.OTHER;
        };
    }

    public record ManageMedicineCommand(
            Long inventoryId,
            Long medicineId,
            String name,
            String category,
            BigDecimal price,
            Integer stockQty,
            String medicineType,
            LocalDate expiryDate,
            Integer lowStockThreshold
    ) {
    }

    public record SupplyRestockCommand(
            Long supplierId,
            Long inventoryId,
            Long medicineId,
            Integer quantity,
            LocalDate expectedDate
    ) {
    }
}
