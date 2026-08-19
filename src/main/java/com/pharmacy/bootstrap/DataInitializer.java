package com.pharmacy.bootstrap;

import com.pharmacy.model.Admin;
import com.pharmacy.model.Customer;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Pharmacist;
import com.pharmacy.model.Supplier;
import com.pharmacy.pattern.factory.MedicineFactorySelector;
import com.pharmacy.repository.AdminRepository;
import com.pharmacy.repository.CustomerRepository;
import com.pharmacy.repository.MedicineRepository;
import com.pharmacy.repository.PharmacistRepository;
import com.pharmacy.repository.SupplierRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final MedicineRepository medicineRepository;
    private final MedicineFactorySelector medicineFactorySelector;
    private final AdminRepository adminRepository;
    private final PharmacistRepository pharmacistRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            MedicineRepository medicineRepository,
            MedicineFactorySelector medicineFactorySelector,
            AdminRepository adminRepository,
            PharmacistRepository pharmacistRepository,
            SupplierRepository supplierRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.medicineRepository = medicineRepository;
        this.medicineFactorySelector = medicineFactorySelector;
        this.adminRepository = adminRepository;
        this.pharmacistRepository = pharmacistRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedMedicine("Paracetamol 500", "Pain Relief", "Acme Pharma", "3.50", 120, 10, "TABLET", 10);
        seedMedicine("Cough Syrup DX", "Cold & Cough", "Nova Remedies", "6.75", 45, 10, "SYRUP", 6);
        seedMedicine("Amoxicillin 250", "Antibiotic", "Zen Labs", "8.90", 70, 12, "OTHER", 9);
        seedMedicine("Ibuprofen 400", "Pain Relief", "HealWell", "4.25", 95, 10, "TABLET", 11);
        seedMedicine("Cetirizine", "Allergy", "Aster Pharma", "2.80", 60, 10, "TABLET", 8);
        seedMedicine("Vitamin C Syrup", "Supplements", "NutraPlus", "7.10", 38, 8, "SYRUP", 7);
        seedMedicine("Azithromycin 500", "Antibiotic", "Medisphere", "12.40", 30, 10, "TABLET", 5);
        seedMedicine("Pantoprazole 40", "Gastric Care", "CoreCure", "5.60", 85, 12, "TABLET", 10);
        seedMedicine("ORS Sachets", "Hydration", "LifeSalt", "1.20", 140, 20, "OTHER", 14);
        seedMedicine("Calcium Plus", "Supplements", "BoneSure", "9.15", 55, 10, "TABLET", 12);
        seedMedicine("Nasal Relief Spray", "Cold & Cough", "BreatheEasy", "10.50", 28, 8, "OTHER", 9);
        seedMedicine("Antacid Gel", "Gastric Care", "Digest Labs", "6.20", 42, 10, "SYRUP", 6);

        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setName("System Admin");
            admin.setEmail("admin@pharmaflow.local");
            admin.setPhone("9000000001");
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setAdminLevel(1);
            adminRepository.save(admin);
        }

        if (pharmacistRepository.count() == 0) {
            Pharmacist pharmacist = new Pharmacist();
            pharmacist.setName("Duty Pharmacist");
            pharmacist.setEmail("pharmacist@pharmaflow.local");
            pharmacist.setPhone("9000000002");
            pharmacist.setUsername("pharmacist");
            pharmacist.setPassword(passwordEncoder.encode("pharma123"));
            pharmacist.setEmployeeId(5001L);
            pharmacist.setLicenseNumber("LIC-PH-5001");
            pharmacistRepository.save(pharmacist);
        }

        if (customerRepository.count() == 0) {
            Customer customer = new Customer();
            customer.setName("Demo Customer");
            customer.setEmail("9000000003@customer.pharmaflow.com");
            customer.setPhone("9000000003");
            customer.setUsername("9000000003");
            customer.setPassword(passwordEncoder.encode("customer123"));
            customer.setCustomerId(9000000003L);
            customerRepository.save(customer);
        }

        if (supplierRepository.count() == 0) {
            Supplier supplier = new Supplier();
            supplier.setName("Prime Supplier");
            supplier.setEmail("supplier@pharmaflow.local");
            supplier.setPhone("9000000004");
            supplier.setUsername("supplier");
            supplier.setPassword(passwordEncoder.encode("supplier123"));
            supplier.setSupplierId(7001L);
            supplier.setCompanyName("Prime Supplier Co.");
            supplier.setContactInfo("support@prime-supplier.local");
            supplierRepository.save(supplier);
        }

        // Prescriptions are not auto-seeded; they should be created by application workflows.
    }

    private void seedMedicine(
            String name,
            String category,
            String manufacturer,
            String price,
            int stockQty,
            int lowStockThreshold,
            String medicineType,
            int expiryMonthsAhead
    ) {
        if (medicineRepository.existsByName(name)) {
            return;
        }
        Medicine medicine = medicineFactorySelector.byType(medicineType).createMedicine(
                name,
                category,
                new BigDecimal(price),
                stockQty,
                LocalDate.now().plusMonths(expiryMonthsAhead),
                lowStockThreshold
        );
        medicine.setManufacturer(manufacturer);
        medicineRepository.save(medicine);
    }
}
