# Pharmacy Inventory and Billing System — Design Document

## 1. System Design Overview

Layered Spring Boot MVC application with role-based workflows for four actors: Admin,
Pharmacist, Customer, Supplier. There is no separate REST API — the Thymeleaf-rendered web
app is the only presentation surface.

- Presentation layer: `com.pharmacy.controller` (web MVC) + `src/main/resources/templates`
- Business layer: `com.pharmacy.service` (actor services, `BillingFacade`, `BillFactory`),
  `com.pharmacy.service.discount` (Strategy), `com.pharmacy.pattern.*` (Factory, Decorator,
  Observer)
- Data layer: `com.pharmacy.model` (JPA entities), `com.pharmacy.repository` (Spring Data)

Runtime flow:

`Controller -> Actor Service -> Repository -> Database`

and for billing specifically:

`BillingFacade -> DiscountStrategy (select %) -> Decorator chain (apply discount, then tax) -> BillFactory (construct) -> Repository`

Diagrams: see [`docs/diagrams/`](docs/diagrams) — `use-cases.puml`, `class-domain.puml`,
`class-services.puml`, and one inset per pattern (`pattern-factory.puml`,
`pattern-billing.puml`, `pattern-observer.puml`, `pattern-facade.puml`).

## 2. Domain Model

### 2.1 User hierarchy

`User` (abstract, `JOINED` inheritance) holds `username`/`password`/`email`/`phone` and an
abstract `roleName()`. `Admin`, `Pharmacist`, `Customer`, `Supplier` extend it. Every role
authenticates through the same `UserRepository.findByUsername` lookup — customers use their
phone number as username, staff use fixed usernames (`admin`, `pharmacist`, `supplier`).
Passwords are BCrypt-hashed at seed time and at self-registration.

### 2.2 Medicine hierarchy

`Medicine` (abstract, `SINGLE_TABLE` inheritance, discriminator column `medicine_type`) defines
shared fields (`name`, `price`, `stockQty`, `expiryDate`, ...) plus two abstract members that
each subclass implements differently:

- `nearExpiryWindowDays()` — `Tablet`/`GenericMedicine`: 30 days, `Syrup`: 45 days (liquids
  degrade faster)
- `unitOfSale()` — `Tablet`: "strip", `Syrup`: "bottle", `GenericMedicine`: "unit"

A medicine's concrete type is fixed at creation (`MedicineFactorySelector.byType(...)`) and
cannot change afterward — the admin edit form only edits shared fields.

### 2.3 Billing/order model

`Order` owns `OrderItem` lines and computes its own subtotal/total (Information Expert). `Bill`
holds `subtotal`/`discountAmount`/`taxAmount`/`total`, one per `Order`. `Payment` is one-to-one
with `Bill`.

### 2.4 Procurement model

`Supplier` submits an `Invoice` with `InvoiceItem` lines; each item produces a `Shipment` that
the supplier later marks delivered/declined/cancelled.

## 3. Design Pattern Integration

Every pattern below does real work — none exist only to be documented.

### 3.1 Factory Method

Package: `com.pharmacy.pattern.factory`

`MedicineFactory` is implemented by `TabletMedicineFactory`, `SyrupMedicineFactory`,
`GenericMedicineFactory`, each of which is the *only* place that names its concrete `Medicine`
subclass (`new Tablet()`, `new Syrup()`, `new GenericMedicine()`). `MedicineFactorySelector`
resolves the right factory from the type string. This is load-bearing because `Medicine` is
abstract with no public constructor path outside its subclasses — `AdminService.createMedicine`
and `DataInitializer` cannot construct a medicine any other way.

### 3.2 Strategy + Decorator (billing)

Strategy selects; Decorator applies. `com.pharmacy.service.discount.DiscountStrategy` has two
implementations — `LoyaltyDiscountStrategy` (`@Order(1)`, tiered by `Customer.loyaltyPoints`:
0 unsupported, 1–99 → 5%, 100–199 → 10%, 200+ → 15%) and `DefaultDiscountStrategy`
(`@Order(99)`, always supports, always 0%). `BillingFacade.resolveDiscount` takes the
first-supporting strategy.

The resolved percent feeds a fixed decorator pipeline in `com.pharmacy.pattern.decorator`:

```
BaseBillAmount(subtotal) -> DiscountDecorator(discountPercent) -> TaxDecorator(pharmacy.tax-percent)
```

Composition order is explicit in `BillingFacade.processCustomerBilling` and matters: discount is
applied to the subtotal, then tax is applied to the discounted amount, not the other way round.
`pharmacy.tax-percent` (default `5`) is configurable in `application.properties`.
`BillFactory.createBill` no longer computes anything — it is pure construction, taking the
already-resolved subtotal/discount/tax/total.

### 3.3 Observer

Package: `com.pharmacy.pattern.observer`. `InventoryAlertSubject` receives every registered
`InventoryObserver` bean via constructor-injected `List<InventoryObserver>` (Spring collects
all implementations automatically). It checks `Medicine.isLowStock()` /
`Medicine.isNearExpiry()` — the latter now polymorphic per medicine subclass — and notifies
observers. `AdminAlertObserver` is the current observer, buffering the last 200 alerts for the
admin dashboard and the scheduled restock job. `BillingFacade`, `AdminService` and
`PharmacistService` all call `notifyLowStockOrExpiry` after any stock change, so alerts fire
from every code path that touches stock, not just one.

### 3.4 Facade

`com.pharmacy.service.BillingFacade` is the single `@Transactional` entry point for billing: it
validates order state, verifies stock, resolves discount, runs the decorator chain, deducts
stock, notifies observers, and processes payment. Controllers call two public methods
(`processCustomerBilling`, `processPayment`) and never touch `OrderRepository`, `BillRepository`,
`BillFactory` or the decorator chain directly.

## 4. GRASP / SOLID notes

Kept brief — these follow from the structure above rather than being separately engineered:

- **Information Expert**: `Order` computes its own totals; `Medicine` knows its own
  low-stock/near-expiry state.
- **Protected Variations**: `DiscountStrategy` and `MedicineFactory` isolate the billing/creation
  flow from new discount rules or medicine types — both are add-a-class extension points.
- **Pure Fabrication**: `BillingFacade` and `BillFactory` are not domain concepts; they exist to
  keep orchestration out of entities and controllers.
- **DIP**: every service depends on constructor-injected interfaces
  (`List<DiscountStrategy>`, `List<InventoryObserver>`, repository interfaces), never on
  concretions it constructs itself.

## 5. Historical note

An earlier draft of this project split ownership four ways (one actor + one pattern per
teammate) for a graded submission. This is now a personal project with a single owner, so that
split has been dropped from the architecture narrative.
