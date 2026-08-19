# Pharmacy Inventory and Billing System — Design Document

## 1. System Design Overview

Layered Spring Boot MVC application with role-based workflows for four actors: Admin,
Pharmacist, Customer, Supplier. There is no separate REST API — the Thymeleaf-rendered web
app is the only presentation surface.

- Presentation layer: `com.pharmacy.controller` (web MVC) + `src/main/resources/templates`
- Business layer: `com.pharmacy.service` (actor services, `BillingFacade`, `BillFactory`),
  `com.pharmacy.service.discount` (Strategy), `com.pharmacy.pattern.*` (Factory, Decorator,
  Observer)
- Data layer: `com.pharmacy.model` (JPA entities), `com.pharmacy.repository` (Spring Data),
  `src/main/resources/db/migration` (Flyway schema migrations)

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

`OrderItem` snapshots `unitPrice` and `lineTotal` at order time rather than reading through to
`Medicine.price`. A later price change therefore does not retroactively alter historical orders
or bills — the order records what the customer was actually quoted.

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

## 5. Data integrity and concurrency

### 5.1 Money representation

All monetary fields (`Medicine.price`, `OrderItem.unitPrice`/`lineTotal`, `Order.totalAmount`,
`Bill.*`, `Payment.amount`, `Invoice`/`InvoiceItem` amounts) are `BigDecimal` mapped to
`numeric(12,2)` — never `double`, which cannot represent decimal currency values exactly.

Rounding is explicit and consistent: each decorator in the billing chain rounds to scale 2 with
`RoundingMode.HALF_UP`. `BillingFacade` derives the tax component by *subtraction*
(`total - discountedSubtotal`) rather than recomputing it independently, so the stored
`subtotal`/`discountAmount`/`taxAmount`/`total` always reconcile exactly with no rounding drift
between the components and the sum.

### 5.2 Stock concurrency

`Medicine` carries a JPA `@Version` field, so every stock write becomes
`UPDATE ... WHERE medicine_id = ? AND version = ?` under optimistic locking.

This closes a check-then-act race in `BillingFacade.processCustomerBilling`: stock is validated
by `verifyStockAvailability` and decremented a few lines later. Without versioning, two
pharmacists billing the last unit concurrently would both read `stockQty = 1`, both pass the
check, and both decrement — overselling silently, because `Medicine.reduceStock` clamps at zero
rather than failing. With `@Version`, the second transaction's flush fails; `BillingFacade`
catches `OptimisticLockingFailureException` and rethrows it as an `IllegalStateException`
("stock changed, please retry"), which `GlobalExceptionHandler` renders as a normal user-facing
error. Losing the race is the correct outcome here — the operator re-checks availability rather
than the system silently overselling.

Double-billing a single order is prevented separately and structurally: `app_bills.order_id`
carries a unique constraint, so a duplicate bill fails at the database level even if two
transactions both pass the in-transaction status check.

### 5.3 Authorization

Role-level access is enforced by `SecurityConfig` path matchers plus `@PreAuthorize` on
controllers. Beyond that, operations that act on a specific record verify **ownership**, not just
role: `CustomerService.cancelOrder` and `CustomerService.makePayment` both confirm the target
record belongs to the authenticated customer before proceeding. Without this an authenticated
customer could settle another customer's bill by supplying its ID (an IDOR) — role checks alone
do not prevent horizontal privilege escalation between users of the *same* role.

The H2 console is restricted to `ROLE_ADMIN`. Its CSRF exemption and same-origin frame allowance
exist only so that console can function, and apply to no other path.

### 5.4 Schema management

Flyway owns the schema. `src/main/resources/db/migration/V1__init.sql` is the baseline, and
`spring.jpa.hibernate.ddl-auto=validate` means Hibernate only *verifies* that the entity mappings
match the migrated schema — it never alters it. A mismatch fails startup loudly instead of being
silently patched, and schema changes become reviewable, versioned, and replayable in order.

The baseline was reverse-engineered from the previous `ddl-auto=update` schema (exported via
`jakarta.persistence.schema-generation`) and then cleaned up to give the foreign keys readable
names, rather than being authored schema-first.

Tests run against a throwaway in-memory H2 database (`src/test/resources/application.properties`)
using the same Flyway + `validate` path as production, so they exercise the real schema and
cannot mutate or leak state into the local `./data/pharmacydb` dev database.

## 6. Historical note

An earlier draft of this project split ownership four ways (one actor + one pattern per
teammate) for a graded submission. This is now a personal project with a single owner, so that
split has been dropped from the architecture narrative.
