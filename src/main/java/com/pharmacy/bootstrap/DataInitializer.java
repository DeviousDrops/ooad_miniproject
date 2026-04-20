package com.pharmacy.bootstrap;

import com.pharmacy.model.Admin;
import com.pharmacy.model.Customer;
import com.pharmacy.model.Inventory;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Pharmacist;
import com.pharmacy.model.Prescription;
import com.pharmacy.model.Supplier;
import com.pharmacy.repository.AdminRepository;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.InventoryRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.PharmacistRepository;
import com.pharmacy.repository.PrescriptionRepository;
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
    private final PrescriptionRepository prescriptionRepository;

    public DataInitializer(
            InventoryRepository inventoryRepository,
            MedicineRepository medicineRepository,
            AdminRepository adminRepository,
            PharmacistRepository pharmacistRepository,
            CustomerRepository customerRepository,
            SupplierRepository supplierRepository,
            PrescriptionRepository prescriptionRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.medicineRepository = medicineRepository;
        this.adminRepository = adminRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    public void run(String... args) {
        Inventory inventory = inventoryRepository.findAll().stream().findFirst().orElseGet(() -> inventoryRepository.save(new Inventory()));

        if (medicineRepository.count() == 0) {
            Medicine m1 = new Medicine();
            m1.setName("Paracetamol 500");
            m1.setCategory("Pain Relief");
            m1.setManufacturer("Acme Pharma");
            m1.setPrice(new BigDecimal("3.50"));
            m1.setStockQty(120);
            m1.setLowStockThreshold(10);
            m1.setMedicineType(Medicine.MedicineType.TABLET);
            m1.setExpiryDate(LocalDate.now().plusMonths(10));
            m1.setInventory(inventory);
            medicineRepository.save(m1);

            Medicine m2 = new Medicine();
            m2.setName("Cough Syrup DX");
            m2.setCategory("Cold & Cough");
            m2.setManufacturer("Nova Remedies");
            m2.setPrice(new BigDecimal("6.75"));
            m2.setStockQty(45);
            m2.setLowStockThreshold(10);
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
            admin.setAdminLevel(1);
            adminRepository.save(admin);
        }

        if (pharmacistRepository.count() == 0) {
            Pharmacist pharmacist = new Pharmacist();
            pharmacist.setName("Duty Pharmacist");
            pharmacist.setEmail("pharmacist@pharmaflow.local");
            pharmacist.setPhone("9000000002");
            pharmacist.setPassword("pharma123");
            pharmacist.setEmployeeId(5001L);
            pharmacist.setLicenseNumber("LIC-PH-5001");
            pharmacistRepository.save(pharmacist);
        }

        if (customerRepository.count() == 0) {
            Customer customer = new Customer();
            customer.setName("Walk-in Customer");
            customer.setEmail("customer@pharmaflow.local");
            customer.setPhone("9000000003");
            customer.setPassword("customer123");
            customer.setCustomerId(1001L);
            customer.setLoyaltyPoints(120);
            customer.setAddress("MG Road, Bengaluru");
            customerRepository.save(customer);
        }

        if (supplierRepository.count() == 0) {
            Supplier supplier = new Supplier();
            supplier.setName("Prime Supplier");
            supplier.setEmail("supplier@pharmaflow.local");
            supplier.setPhone("9000000004");
            supplier.setPassword("supplier123");
            supplier.setSupplierId(7001L);
            supplier.setCompanyName("Prime Supplier Co.");
            supplier.setContactInfo("support@prime-supplier.local");
            supplierRepository.save(supplier);
        }

        if (prescriptionRepository.count() == 0 && customerRepository.count() > 0 && medicineRepository.count() > 0) {
            Customer customer = customerRepository.findAll().get(0);
            Medicine medicine = medicineRepository.findAll().get(0);

            Prescription prescription = new Prescription();
            prescription.setCustomer(customer);
            prescription.setMedicine(medicine);
            prescription.setDoctorName("Dr. Sharma");
            prescription.setDosage("1 tablet after food");
            prescription.setNotes("Continue for 5 days");
            prescriptionRepository.save(prescription);
        }
    }
}
