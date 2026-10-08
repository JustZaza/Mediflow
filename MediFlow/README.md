# MediFlow — MVP Implementation

Java + MySQL hospital/pharmacy workflow, built to the MVP scope from the design doc (section 20):

```
Login → Patient → Appointment → Queue → Doctor consultation → Prescription → Pharmacy → Dispensing → Inventory update
```

Excel import/export, full reporting, supplier management UI, and JavaFX screens are **not** included yet — they're the natural next phases once this workflow is solid (see the doc's roadmap).

## What's here

```
database/
  schema.sql       full MySQL schema (all 14 tables + reporting views, matches the design doc's ERD)
  seed.sql         sample users, patients, medicines, inventory, appointments
  queries.sql      the 10 operational queries used by dashboards/reports

src/main/java/com/mediflow/
  model/           Patient, Doctor, Appointment, QueueEntry, MedicalRecord, Prescription,
                    PrescriptionItem, Medicine, InventoryBatch, Dispensing, DispensingItem, User
  dao/             one *DAO per table — all SQL lives here (PreparedStatement only)
  service/         business rules: AuthService, PatientService, AppointmentService, QueueService,
                    MedicalRecordService, PrescriptionService, PharmacyService
  controller/      thin layer a JavaFX view would call — never touches SQL
  database/        DatabaseConnection (MySQL for real use, H2-in-memory for this runnable demo)
  exception/       DuplicatePatientException, InsufficientStockException, ExpiredMedicineException, etc.
  util/            PasswordUtil (BCrypt), ValidationUtil
  Main.java        runs the whole MVP workflow end-to-end, console output, no setup required

src/test/java/com/mediflow/
  PharmacyServiceTest.java   covers 5 scenarios from the design doc's testing table (section 15)

src/main/resources/demo-schema.sql   trimmed H2-compatible schema used only by Main.java and the tests

web-demo/
  index.html       self-contained, no-build browser demo of the full MVP workflow — just open it
```

## Quickest way to see the workflow

Open `web-demo/index.html` in any browser. No install, no server — pick a role (Receptionist,
Doctor, Pharmacist) and click through the same Login → Patient → Appointment → Queue →
Consultation → Prescription → Dispensing → Inventory flow the Java code implements, with the
same business rules (no double-booking a doctor, priority queue ordering, no negative stock,
no dispensing past expiry).

## Why H2 instead of MySQL for the demo

This sandbox has no network access and no MySQL server, so `Main.java` and the tests run against
an **in-memory H2 database** (`DatabaseConnection.Mode.H2_DEMO`) that mirrors the real schema, purely
so the workflow is runnable with zero setup. All SQL in the DAOs is standard JDBC/ANSI-ish SQL that
also runs on MySQL — the one thing to watch is MySQL-specific syntax if you extend a DAO (e.g. avoid
`LIMIT ?,?`-style MySQL shortcuts if you want both dialects to keep working).

**To run against real MySQL:**
1. `mysql -u root -p < database/schema.sql`
2. `mysql -u root -p mediflow < database/seed.sql`
3. In `DatabaseConnection.java`, change `MODE` to `Mode.MYSQL` and fill in your credentials.

## Building & running

This project needs a JDK (17+) and Maven — neither is available in the sandbox that generated this
code, so **it has not been compiled here**. Structure and logic follow the design doc closely and
each class is self-contained, but please compile locally before relying on it:

```bash
mvn compile
mvn test              # runs PharmacyServiceTest against the H2 demo schema
mvn exec:java -Dexec.mainClass=com.mediflow.Main   # or: mvn package && java -jar target/mediflow-0.1.0-MVP.jar
```

## Business rules implemented (section 6 of the design doc)

| Rule | Where enforced |
|---|---|
| Patient ID must be unique | `PatientService.register` → `DuplicatePatientException` |
| DOB cannot be in the future | `ValidationUtil.requireDobNotInFuture` |
| No overlapping doctor appointments | `AppointmentService.book` → `AppointmentDAO.hasConflict` |
| Cancelled appointments can't be checked in | `AppointmentService.checkIn` |
| Prescription needs ≥1 item | `PrescriptionService.create` |
| Only doctors can prescribe | `PrescriptionService.create` (checks `User.Role`) |
| Stock can't go negative | `InventoryDAO.deductQuantity` (conditional `UPDATE ... WHERE quantity >= ?`) |
| Dispense qty can't exceed stock | `PharmacyService.fulfillItem` → `InsufficientStockException` |
| Expired medicine can't be dispensed | `PharmacyService.fulfillItem` → `ExpiredMedicineException` |
| Low stock = qty ≤ minimum | `InventoryDAO.findLowStock` |
| Dispensing is transactional | `PharmacyService.dispense` (manual commit/rollback around header+items+stock deduction) |

## Queue priority (section 7)

`QueueService` loads today's waiting patients into a `java.util.PriorityQueue<QueueEntry>`.
`QueueEntry` implements `Comparable` so EMERGENCY beats HIGH beats NORMAL, and ties are broken by
queue number (earlier check-in first) — giving correct "who's next" behavior with O(log n)
insert/poll instead of scanning a plain list.

## Next steps toward the full scope

- Wire `controller/` classes into an actual JavaFX UI (section 10's suggested stack)
- `util/ExcelImporter` / `ExcelExporter` using the already-included Apache POI dependency (FR-09)
- `DoctorDAO`, `SupplierDAO`, and admin-facing user management (FR-01 admin actions)
- Dashboard/report screens backed by `database/queries.sql` and the `v_low_stock` / `v_expiring_soon` / `v_todays_queue` views
