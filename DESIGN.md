# Pharmacy Inventory and Billing System - Design Document

## 1. System Design Overview

This project follows a layered Spring Boot architecture with MVC and role-based workflows.

- Presentation layer:
  - Web controllers under `com.pharmacy.controller`
  - REST controllers under `com.pharmacy.controller.api`
  - Thymeleaf templates under `src/main/resources/templates`
- Business layer:
  - Core services under `com.pharmacy.service`, `com.pharmacy.service.billing`, and `com.pharmacy.service.inventory`
  - Actor-focused API services under `com.pharmacy.service.actor`
- Data layer:
  - JPA entities under `com.pharmacy.model`
  - Repositories under `com.pharmacy.repository`

Runtime flow:

`Controller -> Service -> Repository -> Database`

and for business events:

`Service action -> Pattern component (factory/decorator/observer/strategy) -> Persist -> Response`

## 2. Functional Design (Key Functions)

### 2.1 Application and Bootstrap

- `PharmacyApplication.main`: boots Spring application and scheduler support.
- `DataInitializer.run`: seeds default inventory, medicine, actors, and sample prescription data.
- `OpenApiConfig.pharmacyOpenApi`: publishes API metadata for OpenAPI docs.

### 2.2 Inventory Functions

Core class: `com.pharmacy.service.inventory.InventoryService`

- `manageMedicineInventory`: create/update medicine and trigger low-stock/expiry alerts.
- `listMedicines`, `searchMedicines`: browse inventory catalog.
- `verifyStock`, `verifyBulkStock`: stock validation for operations and billing.
- `updateInventoryStatus`: touch inventory state after updates.
- `supplyRestock`: create pending shipment from supplier.
- `shipmentVerification`: verify shipment, increase stock, and trigger alerts.
- `deductStockForBilling`: reduce stock after bill generation.
- `latestAlerts`: read current admin-facing inventory alerts.

API surface:

- `/api/inventory/medicines`
- `/api/inventory/verify-stock`
- `/api/inventory/alerts`
- `/api/inventory/status/{inventoryId}`

### 2.3 Billing Functions

Core class: `com.pharmacy.service.billing.BillingService`

- `placeOrder`: validates customer and item stock, creates order items, persists order.
- `generateBill`: computes subtotal + tax + discount and persists bill.
- `processPayment`: records payment and marks order as paid.
- `customerBillHistory`: returns historical billing records.

API surface:

- `/api/billing/orders`
- `/api/billing/bills/{orderId}`
- `/api/billing/payments/{billId}`
- `/api/billing/history/{customerId}`

### 2.4 Admin Functions

Web and service classes: `AdminController`, `AdminService`

- Dashboard view of medicines, inventories, alerts, and automation history.
- `manageMedicineInventory`, `deleteMedicine`: medicine governance.
- `generateSalesAnalytics`, `generateInventoryReport`: reporting.
- `updateMedicineInventory`: manual stock adjustment.
- `automateMedicineSupply` (scheduled): periodic restock recommendation activity.

API surface:

- `/api/admin/manage-medicine`
- `/api/admin/sales-analytics`
- `/api/admin/automate-supply`

### 2.5 Pharmacist Functions

Web and service classes: `PharmacistController`, `PharmacistService`

- `verifyStock`: verifies medicine availability.
- `processCustomerBilling`: executes end-to-end customer billing flow.
- `updateInventoryStatus`: stock adjustments after dispensing operations.
- `applyLoyaltyDiscount`: role-level helper to derive discount bands.

API surface:

- `/api/pharmacist/verify-stock`
- `/api/pharmacist/process-billing/{orderId}`
- `/api/pharmacist/update-inventory-status/{inventoryId}`

### 2.6 Customer Functions

Web and service classes: `CustomerController`, `CustomerService`

- `searchMedicines`: medicine discovery.
- `placeOrder`: customer order creation.
- `viewPrescriptionHistory`: prescription tracking.
- `makePayment`: payment submission.
- `viewBillHistory`: billing history retrieval.

API surface:

- `/api/customer/search`
- `/api/customer/place-order`
- `/api/customer/prescriptions/{customerId}`
- `/api/customer/bills/{customerId}`

### 2.7 Supplier Functions

Web and service classes: `SupplierController`, `SupplierService`

- `supplyRestock`: creates restock shipment requests.
- `shipmentVerification`: verifies arrival/completion of shipment.
- `submitDigitalInvoice`: submits invoice and updates inventory through admin service.

API surface:

- `/api/supplier/restock`
- `/api/supplier/verify-shipment/{shipmentId}`
- `/api/supplier/digital-invoice`

## 3. Design Pattern Mapping

### 3.1 Factory Method

Package: `com.pharmacy.pattern.factory`

- `MedicineFactory` (interface)
- `TabletMedicineFactory`, `SyrupMedicineFactory`, `GenericMedicineFactory` (concrete creators)
- `MedicineFactorySelector` (type-based factory resolution)

Usage:

- `InventoryService.manageMedicineInventory` delegates medicine instantiation to selected factory.

Why:

- Centralizes object creation and avoids repetitive construction logic across services/controllers.

### 3.2 Decorator

Package: `com.pharmacy.pattern.decorator`

- `BillAmountComponent` (component)
- `BaseBillAmount` (concrete component)
- `BillAmountDecorator` (base decorator)
- `TaxDecorator`, `DiscountDecorator` (concrete decorators)

Usage:

- `BillingService.generateBill` composes tax and discount behavior at runtime.

Why:

- Enables additive billing rules without changing core subtotal calculation code.

### 3.3 Observer

Primary observer package: `com.pharmacy.pattern.observer`

- `InventoryObserver` (observer contract)
- `AdminAlertObserver` (concrete observer)
- `InventoryAlertSubject` (subject/dispatcher)

Additional observer-style service utility: `com.pharmacy.service.InventoryObserver`

Usage:

- Inventory stock and expiry changes publish alerts to subscribed observers/listeners.

Why:

- Decouples inventory state changes from notification handling.

### 3.4 Facade

Class: `com.pharmacy.service.BillingFacade`

Usage:

- Provides one orchestration entry point for stock validation, bill generation, stock deduction, and payment flow.

Why:

- Simplifies controller/service interaction with a complex billing subsystem.

### 3.5 Strategy

Package: `com.pharmacy.service.discount`

- `DiscountStrategy` (strategy interface)
- `LoyaltyDiscountStrategy`, `DefaultDiscountStrategy` (concrete strategies)

Usage:

- `BillingFacade.resolveDiscount` selects the first matching discount rule based on customer loyalty profile.

Why:

- Supports adding new discount behaviors without modifying existing billing code.

## 4. Contribution Split (4 Members, One Actor + One Pattern Each)

Use this split to keep ownership balanced and clear:

| Member | Actor Ownership | Pattern Ownership | Total Effort |
|---|---|---|---|
| Member 1 | Admin | Factory Method | 25% |
| Member 2 | Pharmacist | Decorator | 25% |
| Member 3 | Supplier | Observer | 25% |
| Member 4 | Customer | Strategy | 25% |

### Member 1 - Admin + Factory Method (25%)

- Actor scope: admin web and API workflows (`/dashboard/admin`, `/api/admin/**`) including medicine governance and reports.
- Pattern scope: `com.pharmacy.pattern.factory` (`MedicineFactory`, concrete factories, selector logic).
- Detailed responsibilities: maintain admin medicine creation/update inputs, keep medicine-type to factory mapping correct, and ensure `InventoryService.manageMedicineInventory` uses consistent factory-driven object creation.
- Detailed responsibilities: validate type fallback behavior (`TABLET`, `SYRUP`, default), keep medicine defaults stable, and document how new medicine types should be added.
- Detailed responsibilities: verify admin-triggered inventory actions do not bypass factory rules.
- Effort allocation: 12% admin flows + 10% factory implementation + 3% integration/testing/documentation.

### Member 2 - Pharmacist + Decorator (25%)

- Actor scope: pharmacist web and API workflows (`/dashboard/pharmacist`, `/api/pharmacist/**`) for stock verification and billing operations.
- Pattern scope: `com.pharmacy.pattern.decorator` (`BaseBillAmount`, `TaxDecorator`, `DiscountDecorator`, base decorator contract).
- Detailed responsibilities: maintain billing composition order inside `BillingService.generateBill` so subtotal, tax, and discount are applied consistently.
- Detailed responsibilities: enforce rounding and percentage boundary behavior in billing paths and verify pharmacist-triggered billing uses decorator calculations correctly.
- Detailed responsibilities: align pharmacist billing requests with generated bill outputs and payment pipeline expectations.
- Effort allocation: 12% pharmacist flows + 10% decorator implementation + 3% integration/testing/documentation.

### Member 3 - Supplier + Observer (25%)

- Actor scope: supplier web and API workflows (`/dashboard/supplier`, `/api/supplier/**`) for restock, shipment verification, and invoice submission.
- Pattern scope: `com.pharmacy.pattern.observer` (`InventoryAlertSubject`, `InventoryObserver`, `AdminAlertObserver`).
- Detailed responsibilities: ensure supplier-driven stock changes (especially shipment verification) trigger alert publication when low-stock or near-expiry conditions are met.
- Detailed responsibilities: maintain observer notification quality, avoid duplicate/noisy alert events, and validate alert history visibility through inventory/admin views.
- Detailed responsibilities: keep restock/verification event handling synchronized with observer dispatch rules.
- Effort allocation: 12% supplier flows + 10% observer implementation + 3% integration/testing/documentation.

### Member 4 - Customer + Strategy (25%)

- Actor scope: customer web and API workflows (`/dashboard/customer`, `/api/customer/**`) for medicine search, order placement, payment, and history views.
- Pattern scope: `com.pharmacy.service.discount` (`DiscountStrategy`, `LoyaltyDiscountStrategy`, `DefaultDiscountStrategy`).
- Detailed responsibilities: maintain discount rule selection in `BillingFacade.resolveDiscount` and keep loyalty slabs accurate and extendable.
- Detailed responsibilities: validate customer order/payment scenarios against expected discount outcomes and prevent regressions when adding new strategy rules.
- Detailed responsibilities: define extension guidance for future discount policies without modifying existing strategy implementations.
- Effort allocation: 12% customer flows + 10% strategy implementation + 3% integration/testing/documentation.

Ownership rules:

- Exactly one primary actor per member.
- Exactly one primary pattern per member.
- `BillingFacade` (Facade pattern) remains shared integration glue and is coordinated by Members 2 and 4 during billing integration.

## 5. Notes

- Empty/unimplemented source folders were removed for a cleaner package structure.
- Test folder and generated `target` build output were removed as requested.
