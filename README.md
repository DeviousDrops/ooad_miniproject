# Pharmacy Inventory & Billing System

Spring Boot MVC application for a role-based pharmacy management workflow covering:
- Admin
- Pharmacist
- Customer
- Supplier

The project includes web dashboards, REST APIs, persistent H2 storage, billing and inventory coordination, supplier procurement, and multiple OOAD concepts and design patterns.

## Tech Stack
- Java 21
- Spring Boot 3
- Spring MVC
- Spring Data JPA
- Spring Security
- Thymeleaf
- H2 file-based database

## Current Functional Scope
- Admin can add, edit, and delete medicines, monitor low-stock alerts, view supplier bills, pay or decline pending supplier bills, and track shipment delivery status.
- Pharmacist can verify stock, update inventory quantities, process customer billing, and decline pending customer orders.
- Customer can sign up using a 10-digit phone number, search medicines, place multi-medicine orders, cancel pending orders, view bills, and make payments.
- Supplier can generate multi-medicine procurement bills, view created shipment orders, update delivery status, and cancel pending supplier bills.

## Main Modules Implemented
- Authentication and role-based routing:
  `src/main/java/com/pharmacy/security`
- Web controllers and dashboards:
  `src/main/java/com/pharmacy/controller`
  `src/main/resources/templates`
- Core entities and domain model:
  `src/main/java/com/pharmacy/model`
- Business logic:
  `src/main/java/com/pharmacy/service`
- REST APIs:
  `src/main/java/com/pharmacy/controller/api`
- Persistence:
  `src/main/java/com/pharmacy/repository`
- Startup/bootstrap:
  `src/main/java/com/pharmacy/bootstrap`
- OOAD pattern implementations:
  `src/main/java/com/pharmacy/pattern`

## Design Patterns Used
- Factory Method
  Used for medicine creation through `MedicineFactory`, `TabletMedicineFactory`, `SyrupMedicineFactory`, `GenericMedicineFactory`, and `MedicineFactorySelector`.
- Decorator
  Used in bill amount computation with `BaseBillAmount`, `TaxDecorator`, and `DiscountDecorator`.
- Observer
  Used for low-stock alerting with `InventoryObserver`, `InventoryAlertSubject`, and `AdminAlertObserver`.
- Facade
  Used in `BillingFacade` to coordinate stock verification, bill generation, order status update, payment handling, and inventory reduction.

## Object-Oriented Design Analysis

### Domain Modelling
The system is structured around clear domain entities:
- `User` as an abstract superclass
- role-specific subclasses: `Admin`, `Pharmacist`, `Customer`, `Supplier`
- pharmacy operations entities: `Medicine`, `Inventory`, `Order`, `OrderItem`, `Bill`, `Payment`
- procurement entities: `Invoice`, `InvoiceItem`, `Shipment`, `Report`

This gives a strong OO decomposition where responsibilities are tied to real business objects instead of being mixed into controllers.

### Layered OOAD Structure
- Presentation layer:
  Thymeleaf dashboards and Spring MVC controllers
- Application/service layer:
  role-specific services and orchestration services such as `BillingFacade`
- Domain layer:
  entity classes and domain behavior such as order status, payment processing, stock adjustments
- Persistence layer:
  Spring Data JPA repositories

This separation improves maintainability, testing, and future extensibility.

## GRASP Principles Reflected In The Project

### Information Expert
- `Order` calculates totals from its own items.
- `Payment` manages payment status transitions.
- `Invoice` summarizes item and shipment information.
- `Medicine` and inventory-related services handle stock changes.

The class with the required information is given the responsibility to act on it.

### Controller
- `AdminController`, `PharmacistController`, `CustomerController`, and `SupplierController` act as system operation controllers for their respective actor flows.
- API controllers play the same role for REST endpoints.

### Low Coupling
- Controllers delegate to services instead of directly manipulating repositories and entities.
- Repositories are injected instead of instantiated.
- Pattern-specific packages isolate creation, alerting, and billing behaviors.

### High Cohesion
- `CustomerService` focuses on customer use cases.
- `PharmacistService` focuses on order billing and stock operations.
- `SupplierService` focuses on procurement bill and shipment workflows.
- `AdminService` focuses on inventory governance, supplier bill handling, alerts, and reporting.

### Polymorphism
- User roles are represented through inheritance from `User`.
- discount handling uses the `DiscountStrategy` abstraction.
- medicine creation varies through factory implementations.

### Pure Fabrication
- `BillingFacade` is a non-domain orchestration object introduced to keep controllers and entities simpler.
- service classes encapsulate business workflows that do not belong inside one single entity.

### Indirection
- services and repositories sit between UI/controllers and persistence.
- the facade and strategy abstractions reduce direct coupling between modules.

### Protected Variations
- `DiscountStrategy` protects the billing flow from changes in discount rules.
- factory abstractions protect the system from changes in medicine creation logic.
- observer-based alerting protects the inventory flow from future notification changes.

## SOLID Principles Reflected In The Project

### Single Responsibility Principle
- Each service class is actor- or workflow-focused.
- Controllers handle request/response flow only.
- repositories are limited to persistence access.
- bootstrap classes handle startup seeding and schema repair only.

### Open/Closed Principle
- New medicine types can be added by extending factory implementations.
- New discount rules can be introduced by adding another `DiscountStrategy`.
- Alert behavior can be extended through observer implementations.

### Liskov Substitution Principle
- `Admin`, `Customer`, `Pharmacist`, and `Supplier` substitute cleanly for the abstract `User` concept.

### Interface Segregation Principle
- narrow role-based contracts such as `AdminOperations`, `CustomerOperations`, `PharmacistOperations`, and `SupplierOperations` avoid forcing actors to depend on unrelated methods.

### Dependency Inversion Principle
- services depend on repository abstractions and strategy interfaces injected by Spring.
- high-level billing logic depends on abstractions like `DiscountStrategy` rather than hard-coded concrete rules.

## 4-Member Implementation Split
This is a practical implementation-wise split based on what is already built in the repository. Replace `Member 1` to `Member 4` with actual team names in your submission if needed.

### Member 1: Admin + Inventory Governance
Scope handled:
- Admin dashboard and medicine management UI
- add, edit, and delete medicine flows
- low-stock alert integration
- inventory synchronization with medicine stock
- sales and inventory report generation
- supplier bill approval flow from admin side
- admin-side shipment visibility

Key implementation areas:
- `src/main/java/com/pharmacy/controller/AdminController.java`
- `src/main/java/com/pharmacy/service/AdminService.java`
- `src/main/resources/templates/dashboard/admin.html`
- `src/main/java/com/pharmacy/service/InventoryObserver.java`
- `src/main/java/com/pharmacy/repository/MedicineRepository.java`
- `src/main/java/com/pharmacy/repository/InventoryRepository.java`

OOAD contribution:
- Applied GRASP Controller through `AdminController`
- Applied Information Expert in inventory and medicine coordination
- Applied SRP by keeping admin concerns in `AdminService`

### Member 2: Customer + Pharmacist Order/Billing Flow
Scope handled:
- customer sign up and phone-number-based identity
- customer medicine search and ordering flow
- customer order cancellation
- bill history and payment flow
- pharmacist dashboard
- stock verification
- pharmacist billing process
- pharmacist order decline flow

Key implementation areas:
- `src/main/java/com/pharmacy/controller/CustomerController.java`
- `src/main/java/com/pharmacy/controller/PharmacistController.java`
- `src/main/java/com/pharmacy/controller/RegistrationController.java`
- `src/main/java/com/pharmacy/service/CustomerService.java`
- `src/main/java/com/pharmacy/service/PharmacistService.java`
- `src/main/java/com/pharmacy/service/BillingFacade.java`
- `src/main/resources/templates/dashboard/customer.html`
- `src/main/resources/templates/dashboard/pharmacist.html`

OOAD contribution:
- Applied Facade to centralize complex billing steps
- Applied High Cohesion by separating customer and pharmacist use cases
- Applied SOLID through service-layer orchestration and dependency injection

### Member 3: Supplier Procurement + Shipment Tracking
Scope handled:
- supplier dashboard
- multi-medicine supplier bill generation
- invoice item modelling
- automatic shipment creation from supplier bills
- shipment delivery updates
- supplier-side cancel flow
- admin-side decline flow for supplier invoices
- synchronized invoice and shipment status visibility across supplier and admin dashboards

Key implementation areas:
- `src/main/java/com/pharmacy/controller/SupplierController.java`
- `src/main/java/com/pharmacy/service/SupplierService.java`
- `src/main/java/com/pharmacy/model/Invoice.java`
- `src/main/java/com/pharmacy/model/InvoiceItem.java`
- `src/main/java/com/pharmacy/model/Shipment.java`
- `src/main/resources/templates/dashboard/supplier.html`

OOAD contribution:
- Applied Information Expert in `Invoice` and `Shipment`
- Applied Low Coupling between supplier UI and procurement persistence through service classes
- Applied Protected Variations by isolating procurement status handling in dedicated models and services

### Member 4: Security, Persistence, APIs, Bootstrap, and Pattern Integration
Scope handled:
- Spring Security configuration
- role-based authentication and redirect logic
- persistent H2 database setup
- startup data seeding
- H2 schema repair for enum evolution
- REST API endpoints for admin, customer, pharmacist, supplier, billing, and inventory
- integration of Factory, Decorator, Observer, and Strategy pattern packages

Key implementation areas:
- `src/main/java/com/pharmacy/security/SecurityConfig.java`
- `src/main/java/com/pharmacy/bootstrap/DataInitializer.java`
- `src/main/java/com/pharmacy/bootstrap/H2SchemaRepair.java`
- `src/main/java/com/pharmacy/controller/api`
- `src/main/java/com/pharmacy/pattern`
- `src/main/java/com/pharmacy/service/discount`
- `src/main/resources/application.properties`

OOAD contribution:
- Applied DIP heavily through Spring dependency injection
- Applied OCP via strategies, observers, and factories
- Supported architectural consistency across modules

## Suggested Viva / Report Summary
If you need a short explanation for presentation:

This project is a role-based pharmacy management system designed using OOAD concepts. The implementation is split across four main actor/workflow modules: Admin, Customer-Pharmacist, Supplier, and Core Platform/API/Security. The design uses MVC architecture, domain modelling, service-layer orchestration, and repository-based persistence. GRASP principles such as Controller, Information Expert, Low Coupling, and High Cohesion are visible throughout the project, while SOLID principles are reflected in the service abstractions, injected dependencies, role-specific contracts, and extensible strategy/factory/observer implementations.

## Running The Project
If Maven is installed:

```bash
mvn spring-boot:run
```

## Application URLs
- Home: `http://localhost:8080/`
- Login: `http://localhost:8080/login`
- H2 Console: `http://localhost:8080/h2-console`

## Current H2 Configuration
- JDBC URL: `jdbc:h2:file:./data/pharmacydb;DB_CLOSE_ON_EXIT=FALSE`
- Username: `sa`
- Password: empty

## Default Logins
- Admin: `admin / admin123`
- Pharmacist: `pharmacist / pharma123`
- Supplier: `supplier / supplier123`
- Customer: create through sign up using a 10-digit phone number

## Notes
- Customer records are persisted after sign up.
- The project uses file-based H2, so data survives restarts.
- If enum-related schema issues occur after changing statuses, restart once so `H2SchemaRepair` can update the stored schema.
