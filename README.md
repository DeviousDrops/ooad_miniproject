# Pharmacy Inventory & Billing System

Spring Boot MVC application for a role-based pharmacy management workflow covering Admin,
Pharmacist, Customer, and Supplier. Personal project — no REST API surface, no grading rubric;
see [`DESIGN.md`](DESIGN.md) for the architecture writeup and [`docs/diagrams/`](docs/diagrams)
for PlantUML use-case, class, and per-pattern diagrams.

## Tech Stack
- Java 21
- Spring Boot 3.5
- Spring MVC + Thymeleaf
- Spring Data JPA + Spring Security (BCrypt, DB-backed)
- Flyway (versioned schema migrations)
- H2 file-based database

## Functional Scope
- **Admin**: create/edit/delete medicines (Tablet/Syrup/Generic), monitor low-stock and
  near-expiry alerts, view and pay/decline supplier invoices, run sales/inventory reports.
- **Pharmacist**: verify stock, process customer billing, decline pending orders, adjust stock
  manually.
- **Customer**: sign up with a 10-digit phone number, search medicines, place multi-item orders,
  cancel pending orders, pay bills, view bill history.
- **Supplier**: submit multi-medicine procurement invoices, track shipment status, cancel pending
  invoices.
- **Scheduler**: a `@Scheduled` job reads the latest low-stock alerts every 60s and logs a
  restock recommendation — the one use case with no human actor.

## Module Map
- Security: `src/main/java/com/pharmacy/security`
- Web controllers + dashboards: `src/main/java/com/pharmacy/controller`, `src/main/resources/templates`
- Domain entities: `src/main/java/com/pharmacy/model`
- Actor services + billing orchestration: `src/main/java/com/pharmacy/service`
- Discount strategies: `src/main/java/com/pharmacy/service/discount`
- Pattern implementations: `src/main/java/com/pharmacy/pattern` (`factory`, `decorator`, `observer`)
- Repositories: `src/main/java/com/pharmacy/repository`
- Schema migrations: `src/main/resources/db/migration`
- Startup seeding: `src/main/java/com/pharmacy/bootstrap`
- Architecture diagrams: `docs/diagrams`

## Design Patterns
- **Factory Method** — `MedicineFactory`/`MedicineFactorySelector` are the only path to
  constructing a `Tablet`, `Syrup`, or `GenericMedicine` (`Medicine` itself is abstract).
- **Strategy + Decorator** — `DiscountStrategy` selects a discount percent;
  `BaseBillAmount -> DiscountDecorator -> TaxDecorator` applies it and then tax, in that order.
- **Observer** — `InventoryAlertSubject` notifies every registered `InventoryObserver`
  (currently `AdminAlertObserver`) whenever stock drops low or a medicine nears expiry.
- **Facade** — `BillingFacade` is the single entry point for the billing workflow.

Details and rationale for each: [`DESIGN.md`](DESIGN.md) §3.

## Running the Project

```bash
mvn spring-boot:run
```

The H2 database (`data/pharmacydb.mv.db`) is not committed. Flyway creates the schema from
`db/migration` on first startup, then `DataInitializer` seeds 12 medicines across the three
subclasses and 4 users if the tables are empty.

Schema changes go in a new `db/migration/V<n>__<description>.sql` file — never by editing an
applied migration, and never by letting Hibernate alter the schema
(`ddl-auto` is `validate`, so a mismatch between entities and migrations fails startup).

## Running Tests

```bash
mvn test
```

Tests run against a throwaway in-memory H2 database using the same Flyway migrations as
production, so they never touch `data/pharmacydb`.

## Application URLs
- Home: `http://localhost:8080/`
- Login: `http://localhost:8080/login`
- H2 Console: `http://localhost:8080/h2-console` — **requires an admin login**
  (JDBC URL `jdbc:h2:file:./data/pharmacydb`, user `sa`, no password)

## Default Logins
| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Pharmacist | `pharmacist` | `pharma123` |
| Supplier | `supplier` | `supplier123` |
| Customer | `9000000003` (seeded demo) | `customer123` |

Customers can also self-register with any 10-digit phone number, which becomes their username.

## Configuration
- `pharmacy.tax-percent` (`application.properties`, default `5`) — tax percent applied after
  discount in the billing decorator chain.

## Correctness Notes
- **Money** is `BigDecimal`/`numeric(12,2)` throughout, rounded `HALF_UP` at scale 2. Order lines
  snapshot the unit price, so later price edits don't rewrite historical bills.
- **Stock** is guarded by JPA optimistic locking (`@Version` on `Medicine`), so two pharmacists
  billing the last unit concurrently can't oversell — the loser gets a retry error.
- **Ownership** is checked on record-level customer actions (pay, cancel), not just role, so one
  customer can't act on another's bills or orders.

See [`DESIGN.md`](DESIGN.md) §5 for the reasoning behind each.
