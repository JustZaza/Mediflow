# Backend for the Doctor and Pharmacist tabs

Your "Not Found" on **Today's Queue, Patient History, Prescriptions and Inventory** means the page is calling URLs
your Spring project doesn't have yet. These are the missing ones:

| Page uses | Endpoint | Status |
|---|---|---|
| Doctor, Pharmacist | `GET /api/medicines` | new |
| Doctor | `GET /api/medical-records` | new |
| Doctor, Pharmacist | `GET /api/prescriptions` | new |
| Pharmacist | `GET /api/dispensing` | new |
| Doctor, Pharmacist | `GET /api/inventory` | new |
| Doctor (Save consultation) | `POST /api/consultations` | new |
| Pharmacist (Dispense) | `POST /api/prescriptions/{id}/dispense` | new |

`/api/queue`, `/api/queue/next`, `/api/doctors` and `/api/appointments` you already have.
Written against the DAO and service classes in the project I gave you. I can't compile it here, so tell me about any red lines.

**Where files go:** every snippet says which existing class to add to. The only new file is
`controller/ClinicalRestController.java` (step 5). Keep these `.md` guides in a `docs/` folder at the project root
(next to `pom.xml`); Spring ignores them. Do not put them under `src/`.

---

## 1. `MedicalRecordDAO` — add

```java
public List<MedicalRecord> findAll() {
    String sql = "SELECT * FROM medical_records ORDER BY created_at DESC";
    List<MedicalRecord> results = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        while (rs.next()) results.add(mapRow(rs));
        return results;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to load medical records", e);
    }
}
```

## 2. `PrescriptionDAO` — add (all prescriptions, each with its items)

```java
public List<Prescription> findAll() {
    String sql = "SELECT * FROM prescriptions ORDER BY prescribed_at DESC";
    String itemSql = "SELECT pi.*, m.name AS medicine_name FROM prescription_items pi " +
            "JOIN medicines m ON m.medicine_id = pi.medicine_id";
    List<Prescription> results = new ArrayList<>();
    java.util.Map<Integer, Prescription> byId = new java.util.HashMap<>();
    try (Connection conn = DatabaseConnection.getConnection()) {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Prescription p = mapRow(rs);
                results.add(p);
                byId.put(p.getPrescriptionId(), p);
            }
        }
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(itemSql)) {
            while (rs.next()) {
                Prescription p = byId.get(rs.getInt("prescription_id"));
                if (p != null) p.addItem(mapItem(rs));
            }
        }
        return results;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to load prescriptions", e);
    }
}
```

## 3. `InventoryDAO` — add

```java
public List<InventoryBatch> findAll() {
    String sql = "SELECT i.*, m.name AS medicine_name FROM inventory i " +
            "JOIN medicines m ON m.medicine_id = i.medicine_id ORDER BY m.name, i.expiry_date";
    List<InventoryBatch> results = new ArrayList<>();
    try (Connection conn = DatabaseConnection.getConnection();
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        while (rs.next()) results.add(mapRow(rs));
        return results;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to load inventory", e);
    }
}
```

## 4. `DispensingDAO` — add (and these imports: `com.mediflow.database.DatabaseConnection`, `com.mediflow.exception.DatabaseException`, `java.util.*`)

```java
public List<Dispensing> findAll() {
    String sql = "SELECT * FROM dispensing ORDER BY dispensed_at DESC";
    String itemSql = "SELECT * FROM dispensing_items";
    List<Dispensing> results = new ArrayList<>();
    Map<Integer, Dispensing> byId = new HashMap<>();
    try (Connection conn = DatabaseConnection.getConnection()) {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Dispensing d = new Dispensing();
                d.setDispensingId(rs.getInt("dispensing_id"));
                d.setPrescriptionId(rs.getInt("prescription_id"));
                d.setPharmacistUserId(rs.getInt("pharmacist_id"));
                Timestamp ts = rs.getTimestamp("dispensed_at");
                d.setDispensedAt(ts != null ? ts.toLocalDateTime() : null);
                results.add(d);
                byId.put(d.getDispensingId(), d);
            }
        }
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(itemSql)) {
            while (rs.next()) {
                Dispensing d = byId.get(rs.getInt("dispensing_id"));
                if (d == null) continue;
                DispensingItem it = new DispensingItem();
                it.setDispensingItemId(rs.getInt("dispensing_item_id"));
                it.setDispensingId(d.getDispensingId());
                it.setMedicineId(rs.getInt("medicine_id"));
                it.setInventoryId(rs.getInt("inventory_id"));
                it.setQuantity(rs.getInt("quantity"));
                d.addItem(it);
            }
        }
        return results;
    } catch (SQLException e) {
        throw new DatabaseException("Failed to load dispensing history", e);
    }
}
```

## 5. New file `controller/ClinicalRestController.java`

One thing to check: `SESSION_KEY` must be the **same string your `LoginController` uses in
`session.setAttribute(...)`**. If your login stores it under a different name (for example `"currentUser"`), change it here.

```java
package com.mediflow.controller;

import com.mediflow.dao.*;
import com.mediflow.exception.AuthenticationException;
import com.mediflow.exception.InvalidPrescriptionException;
import com.mediflow.model.*;
import com.mediflow.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ClinicalRestController {

    private static final String SESSION_KEY = "user";   // <-- must match LoginController

    private final MedicineDAO medicineDAO;
    private final MedicalRecordDAO medicalRecordDAO;
    private final PrescriptionDAO prescriptionDAO;
    private final DispensingDAO dispensingDAO;
    private final InventoryDAO inventoryDAO;
    private final MedicalRecordService medicalRecordService;
    private final PrescriptionService prescriptionService;
    private final PharmacyService pharmacyService;
    private final QueueService queueService;
    private final AppointmentService appointmentService;

    public ClinicalRestController(MedicineDAO medicineDAO, MedicalRecordDAO medicalRecordDAO,
                                  PrescriptionDAO prescriptionDAO, DispensingDAO dispensingDAO,
                                  InventoryDAO inventoryDAO, MedicalRecordService medicalRecordService,
                                  PrescriptionService prescriptionService, PharmacyService pharmacyService,
                                  QueueService queueService, AppointmentService appointmentService) {
        this.medicineDAO = medicineDAO;
        this.medicalRecordDAO = medicalRecordDAO;
        this.prescriptionDAO = prescriptionDAO;
        this.dispensingDAO = dispensingDAO;
        this.inventoryDAO = inventoryDAO;
        this.medicalRecordService = medicalRecordService;
        this.prescriptionService = prescriptionService;
        this.pharmacyService = pharmacyService;
        this.queueService = queueService;
        this.appointmentService = appointmentService;
    }

    private User sessionUser(HttpSession session) {
        Object u = session.getAttribute(SESSION_KEY);
        if (!(u instanceof User user)) throw new AuthenticationException("Please sign in.");
        return user;
    }

    // ---------- read-only lists ----------

    @GetMapping("/medicines")
    public List<Medicine> medicines(HttpSession s) { sessionUser(s); return medicineDAO.findAll(); }

    @GetMapping("/medical-records")
    public List<MedicalRecord> records(HttpSession s) { sessionUser(s); return medicalRecordDAO.findAll(); }

    @GetMapping("/prescriptions")
    public List<Prescription> prescriptions(HttpSession s) { sessionUser(s); return prescriptionDAO.findAll(); }

    @GetMapping("/dispensing")
    public List<Dispensing> dispensing(HttpSession s) { sessionUser(s); return dispensingDAO.findAll(); }

    @GetMapping("/inventory")
    public List<InventoryBatch> inventory(HttpSession s) { sessionUser(s); return inventoryDAO.findAll(); }

    // ---------- consultation (doctor) ----------

    record ItemRequest(int medicineId, String dosage, String frequency, int quantity) {}
    record ConsultationRequest(int queueId, int appointmentId, int patientId, int doctorId,
                               String symptoms, String diagnosis, String notes, List<ItemRequest> items) {}

    @PostMapping("/consultations")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRecord consult(@RequestBody ConsultationRequest r, HttpSession s) {
        User doctor = sessionUser(s);
        if (doctor.getRole() != User.Role.DOCTOR) {
            throw new InvalidPrescriptionException("Only doctors can record consultations");
        }
        MedicalRecord record = medicalRecordService.record(
                r.patientId(), r.doctorId(), r.appointmentId(), r.symptoms(), r.diagnosis(), r.notes());

        if (r.items() != null && !r.items().isEmpty()) {
            List<PrescriptionItem> items = new ArrayList<>();
            for (ItemRequest i : r.items()) {
                items.add(new PrescriptionItem(i.medicineId(), i.dosage(), i.frequency(), "As prescribed", i.quantity()));
            }
            prescriptionService.create(doctor, r.patientId(), r.doctorId(), record.getRecordId(), items);
        }
        queueService.markDone(r.queueId());
        appointmentService.markStatus(r.appointmentId(), Appointment.Status.COMPLETED);
        return record;
    }

    // ---------- dispensing (pharmacist) ----------

    @PostMapping("/prescriptions/{id}/dispense")
    @ResponseStatus(HttpStatus.CREATED)
    public Dispensing dispense(@PathVariable int id, HttpSession s) {
        return pharmacyService.dispense(id, sessionUser(s));   // stock, expiry and the transaction are handled inside
    }
}
```

## 6. Make sure these are Spring beans

Spring can only inject a class that is annotated. Each of these needs the annotation on its class line:

| Annotation | Classes |
|---|---|
| `@Repository` | `MedicineDAO`, `MedicalRecordDAO`, `PrescriptionDAO`, `DispensingDAO`, `InventoryDAO` |
| `@Service` | `MedicalRecordService`, `PrescriptionService`, `PharmacyService`, `QueueService`, `AppointmentService` |

You already did `PatientDAO`, `AppointmentDAO`, `QueueDAO` and the patient and appointment services. If startup says
"required a bean of type X that could not be found", X is missing its annotation. Your `PharmacyService` etc. take their DAOs in
the constructor, which Spring injects automatically.

## 7. Date format

If a record's date shows as `[2026,11,9,10,30]` instead of `"2026-11-09T10:30:00"`, add to `application.properties`:

```
spring.jackson.serialization.write-dates-as-timestamps=false
```

## Try it

1. Restart, sign in as `dr.manamela`. Today's Queue, Patient History and the dashboard should load.
2. Receptionist: book and check in a patient. Doctor: **Call next patient**, fill in the consultation with one medicine, save.
3. Sign in as `pharm1`: the prescription shows under Prescriptions. **Dispense** it, and Inventory drops by that quantity.
4. If Inventory is empty, you need stock rows: use `INSERT INTO inventory ...` (tell me and I'll write 15 test batches and medicines).

## Known limits

- The consultation saves in four steps (record, prescription, queue, appointment), each in its own transaction. If the server dies in
  the middle you could get a record without a prescription. Fine for a demo; a production version wraps them in one `@Transactional`.
- Any signed-in user can call the read-only lists. The consultation and dispensing endpoints already check the role.
