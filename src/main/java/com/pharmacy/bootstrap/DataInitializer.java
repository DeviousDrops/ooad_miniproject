package com.pharmacy.bootstrap;

import com.pharmacy.domain.Admin;
import com.pharmacy.domain.Customer;
import com.pharmacy.domain.Inventory;
import com.pharmacy.domain.Medicine;
import com.pharmacy.domain.Pharmacist;
import com.pharmacy.domain.Supplier;
import com.pharmacy.repository.AdminRepository;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.PharmacistRepository;
import com.pharmacy.repository.SupplierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;
    private final MedicineRepository medicineRepository;
    private final AdminRepository adminRepository;
    private final PharmacistRepository pharmacistRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    public DataInitializer(
            InventoryRepository inventoryRepository,
            MedicineRepository medicineRepository,
            AdminRepository adminRepository,
            PharmacistRepository pharmacistRepository,
            CustomerRepository customerRepository,
            SupplierRepository supplierRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.medicineRepository = medicineRepository;
        this.adminRepository = adminRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public void run(String... args) {
        Inventory inventory = inventoryRepository.findAll().stream().findFirst().orElseGet(() -> inventoryRepository.save(new Inventory()));

        if (medicineRepository.count() == 0) {
            Medicine m1 = new Medicine();
            m1.setName("Paracetamol 500");
            m1.setCategory("Pain Relief");
            m1.setPrice(new BigDecimal("3.50"));
            m1.setStockQty(120);
            m1.setLowStockThreshold(20);
            m1.setMedicineType(Medicine.MedicineType.TABLET);
            m1.setExpiryDate(LocalDate.now().plusMonths(10));
            m1.setInventory(inventory);
            medicineRepository.save(m1);

            Medicine m2 = new Medicine();
            m2.setName("Cough Syrup DX");
            m2.setCategory("Cold & Cough");
            m2.setPrice(new BigDecimal("6.75"));
            m2.setStockQty(45);
            m2.setLowStockThreshold(12);
            m2.setMedicineType(Medicine.MedicineType.SYRUP);
            m2.setExpiryDate(LocalDate.now().plusMonths(6));
            m2.setInventory(inventory);
            medicineRepository.save(m2);
        }

        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setName("System Admin");
            admin.setEmail("admin@pharmaflow.local");
            admin.setPhone("9000000001");
            admin.setPassword("admin123");
            adminRepository.save(admin);
        }

        if (pharmacistRepository.count() == 0) {
            Pharmacist pharmacist = new Pharmacist();
            pharmacist.setName("Duty Pharmacist");
            pharmacist.setEmail("pharmacist@pharmaflow.local");
            pharmacist.setPhone("9000000002");
            pharmacist.setPassword("pharma123");
            pharmacistRepository.save(pharmacist);
        }

        if (customerRepository.count() == 0) {
            Customer customer = new Customer();
            customer.setName("Walk-in Customer");
            customer.setEmail("customer@pharmaflow.local");
            customer.setPhone("9000000003");
            customer.setPassword("customer123");
            customerRepository.save(customer);
        }

        if (supplierRepository.count() == 0) {
            Supplier supplier = new Supplier();
            supplier.setName("Prime Supplier");
            supplier.setEmail("supplier@pharmaflow.local");
            supplier.setPhone("9000000004");
            supplier.setPassword("supplier123");
            supplierRepository.save(supplier);
        }
    }
}
