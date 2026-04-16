# Pharmacy Inventory & Billing System

A Spring Boot MVC application for pharmacy operations with four role-based actors:
- Admin
- Pharmacist
- Customer
- Supplier

## Architecture
- Pattern: MVC (Model, View, Controller)
- Backend: Java 17+, Spring Boot, Spring Data JPA
- Database: H2 (default), compatible with MySQL
- View Layer: Thymeleaf templates

## OOAD Entity Model
Implemented in package `com.pharmacy.domain`:
- User hierarchy: `User` (abstract), `Admin`, `Pharmacist`, `Customer`, `Supplier`
- Inventory & medicine: `Inventory`, `Medicine`
- Transactions: `Order`, `OrderItem`, `Bill`, `Payment`
- Procurement: `Shipment`, `Prescription`, `Invoice`, `Report`

## Required Design Patterns
- Factory Method: `com.pharmacy.pattern.factory`
  - `MedicineFactory`, `TabletMedicineFactory`, `SyrupMedicineFactory`, `MedicineFactorySelector`
- Decorator: `com.pharmacy.pattern.decorator`
  - `BaseBillAmount`, `TaxDecorator`, `DiscountDecorator`
- Observer: `com.pharmacy.pattern.observer`
  - `InventoryObserver`, `AdminAlertObserver`, `InventoryAlertSubject`

## Service Layer
- Billing module: `com.pharmacy.service.billing.BillingService`
- Inventory module: `com.pharmacy.service.inventory.InventoryService`
- Actor services:
  - `AdminService`
  - `PharmacistService`
  - `CustomerService`
  - `SupplierService`

## Repository Layer
Package: `com.pharmacy.repository`
- Billing/inventory repositories included:
  - `MedicineRepository`, `InventoryRepository`, `ShipmentRepository`
  - `OrderRepository`, `OrderItemRepository`, `BillRepository`, `PaymentRepository`
- Additional actor/procurement repositories:
  - `AdminRepository`, `PharmacistRepository`, `CustomerRepository`, `SupplierRepository`
  - `PrescriptionRepository`, `InvoiceRepository`, `ReportRepository`

## UI (Role-specific)
- Home: `/`
- Login: `/login`
- Admin dashboard: `/dashboard/admin`
- Pharmacist dashboard: `/dashboard/pharmacist`
- Customer dashboard: `/dashboard/customer`
- Supplier dashboard: `/dashboard/supplier`

## API Endpoints
- Admin: `/api/admin/**`
- Pharmacist: `/api/pharmacist/**`
- Customer: `/api/customer/**`
- Supplier: `/api/supplier/**`
- Core inventory: `/api/inventory/**`
- Core billing: `/api/billing/**`

## Default Credentials
- Admin: `admin` / `admin123`
- Pharmacist: `pharmacist` / `pharma123`
- Customer: `customer` / `customer123`
- Supplier: `supplier` / `supplier123`

## Run
If Maven is installed:

```bash
mvn spring-boot:run
```

## H2 Console
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:pharmacydb`
- User: `sa`
- Password: (empty)

## Notes for Team of 4
Each actor can be owned by one team member end-to-end:
- Admin: inventory governance + analytics + automation
- Pharmacist: stock verification + billing processing
- Customer: medicine search + order placement + prescription history
- Supplier: restock + shipment verification + digital invoice submission
