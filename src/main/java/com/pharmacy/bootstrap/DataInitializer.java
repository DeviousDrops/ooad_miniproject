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
import java.util.List;

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

        int beforeCount = (int) medicineRepository.count();
        seedMedicine(inventory, "Paracetamol 500", "Pain Relief", "Acme Pharma", "3.50", 120, 10, Medicine.MedicineType.TABLET, 10);
        seedMedicine(inventory, "Cough Syrup DX", "Cold & Cough", "Nova Remedies", "6.75", 45, 10, Medicine.MedicineType.SYRUP, 6);
        seedMedicine(inventory, "Amoxicillin 250", "Antibiotic", "Zen Labs", "8.90", 70, 12, Medicine.MedicineType.OTHER, 9);
        seedMedicine(inventory, "Ibuprofen 400", "Pain Relief", "HealWell", "4.25", 95, 10, Medicine.MedicineType.TABLET, 11);
        seedMedicine(inventory, "Cetirizine", "Allergy", "Aster Pharma", "2.80", 60, 10, Medicine.MedicineType.TABLET, 8);
        seedMedicine(inventory, "Vitamin C Syrup", "Supplements", "NutraPlus", "7.10", 38, 8, Medicine.MedicineType.SYRUP, 7);
        seedMedicine(inventory, "Azithromycin 500", "Antibiotic", "Medisphere", "12.40", 30, 10, Medicine.MedicineType.TABLET, 5);
        seedMedicine(inventory, "Pantoprazole 40", "Gastric Care", "CoreCure", "5.60", 85, 12, Medicine.MedicineType.TABLET, 10);
        seedMedicine(inventory, "ORS Sachets", "Hydration", "LifeSalt", "1.20", 140, 20, Medicine.MedicineType.OTHER, 14);
        seedMedicine(inventory, "Calcium Plus", "Supplements", "BoneSure", "9.15", 55, 10, Medicine.MedicineType.TABLET, 12);
        seedMedicine(inventory, "Nasal Relief Spray", "Cold & Cough", "BreatheEasy", "10.50", 28, 8, Medicine.MedicineType.OTHER, 9);
        seedMedicine(inventory, "Antacid Gel", "Gastric Care", "Digest Labs", "6.20", 42, 10, Medicine.MedicineType.SYRUP, 6);
        if (medicineRepository.count() != beforeCount) {
            inventory.setQuantity(medicineRepository.findByInventory_InventoryId(inventory.getInventoryId()).stream()
                    .map(Medicine::getStockQty)
                    .filter(qty -> qty != null)
                    .mapToInt(Integer::intValue)
                    .sum());
            inventory.touch();
            inventoryRepository.save(inventory);
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
            customer.setPhone("9876543210"); // Phone number is the customer identity and login username.
            customer.setPassword("customer123");
            customer.setCustomerId(Long.parseLong("9876543210"));
            customer.setLoyaltyPoints(120);
            customer.setAddress("MG Road, Bengaluru");
            customerRepository.save(customer);

            // Seed 6 additional customers to meet the 7 minimum requirement
            String[] additionalPhones = {"9876543211", "9876543212", "9876543213", "9876543214", "9876543215", "9876543216"};
            for (int i = 0; i < additionalPhones.length; i++) {
                Customer c = new Customer();
                c.setName("Regular Customer " + (i + 1));
                c.setEmail("customer" + (i + 1) + "@pharmaflow.local");
                c.setPhone(additionalPhones[i]);
                c.setPassword("customer123");
                c.setCustomerId(Long.parseLong(additionalPhones[i]));
                c.setLoyaltyPoints(50 + (i * 10));
                c.setAddress("Avenue " + (i + 1) + ", Bengaluru");
                customerRepository.save(c);
            }
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
            List<Customer> allCustomers = customerRepository.findAll();
            List<Medicine> allMedicines = medicineRepository.findAll();

            if (!allCustomers.isEmpty() && !allMedicines.isEmpty()) {
                Prescription prescription = new Prescription();
                prescription.setCustomer(allCustomers.get(0));
                prescription.setMedicine(allMedicines.get(0));
                prescription.setDoctorName("Dr. Sharma");
                prescription.setDosage("1 tablet after food");
                prescription.setNotes("Continue for 5 days");
                prescriptionRepository.save(prescription);
            }

            // Seed 6 additional prescriptions assigning different medicines to different customers
            for (int i = 1; i <= 6; i++) {
                if (i < allCustomers.size() && i < allMedicines.size()) {
                    Prescription p = new Prescription();
                    p.setCustomer(allCustomers.get(i));
                    p.setMedicine(allMedicines.get(i));
                    p.setDoctorName("Dr. Health " + i);
                    p.setDosage("1 tablet daily");
                    p.setNotes("Standard prescription notes");
                    prescriptionRepository.save(p);
                }
            }
        }
    }

    private void seedMedicine(
            Inventory inventory,
            String name,
            String category,
            String manufacturer,
            String price,
            int stockQty,
            int lowStockThreshold,
            Medicine.MedicineType medicineType,
            int expiryMonthsAhead
    ) {
        if (medicineRepository.existsByName(name)) {
            return;
        }
        Medicine medicine = new Medicine();
        medicine.setName(name);
        medicine.setCategory(category);
        medicine.setManufacturer(manufacturer);
        medicine.setPrice(new BigDecimal(price));
        medicine.setStockQty(stockQty);
        medicine.setLowStockThreshold(lowStockThreshold);
        medicine.setMedicineType(medicineType);
        medicine.setExpiryDate(LocalDate.now().plusMonths(expiryMonthsAhead));
        medicine.setInventory(inventory);
        medicineRepository.save(medicine);
    }
}
