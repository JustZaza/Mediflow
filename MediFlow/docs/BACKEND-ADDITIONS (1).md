# Backend additions for edit, delete and clear error messages

The new `index.html` adds **edit and delete for patients and appointments**. That needs four new endpoints:

| Method | Path | Does |
|---|---|---|
| PUT | `/api/patients/{id}` | update name, phone, email, address, emergency contact |
| DELETE | `/api/patients/{id}` | delete a patient who has no appointments, records, prescriptions or queue entries |
| PUT | `/api/appointments/{id}` | change the time and reason of a `SCHEDULED` appointment (same doctor) |
| DELETE | `/api/appointments/{id}` | delete a `CANCELLED` appointment (cancel it first) |

Search needs no backend change: it filters the lists the page has already loaded.

Add the code below. Nothing existing is replaced, except the optional `ApiExceptionHandler` in step 6.

---

## 1. New exception: `exception/PatientInUseException.java`

```java
package com.mediflow.exception;

public class PatientInUseException extends RuntimeException {
    public PatientInUseException(String message) { super(message); }
}
```

## 2. `PatientDAO` — add two methods

```java
/** True if anything still points at this patient (a delete would break foreign keys). */
public boolean hasLinkedRecords(int patientId) {
    String sql = "SELECT (SELECT COUNT(*) FROM appointments    WHERE patient_id = ?) + " +
                 "       (SELECT COUNT(*) FROM medical_records WHERE patient_id = ?) + " +
                 "       (SELECT COUNT(*) FROM prescriptions   WHERE patient_id = ?) + " +
                 "       (SELECT COUNT(*) FROM queues          WHERE patient_id = ?)";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        for (int i = 1; i <= 4; i++) ps.setInt(i, patientId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to check patient records", e);
    }
}

public void delete(int patientId) {
    String sql = "DELETE FROM patients WHERE patient_id = ?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, patientId);
        ps.executeUpdate();
    } catch (SQLException e) {
        throw new DatabaseException("Failed to delete patient", e);
    }
}
```

## 3. `PatientService` — add `delete`

`update` already exists. Add this next to it (and `import com.mediflow.exception.PatientInUseException;`):

```java
public void delete(int patientId) {
    if (patientDAO.findById(patientId).isEmpty()) {
        throw new IllegalArgumentException("Patient not found");
    }
    if (patientDAO.hasLinkedRecords(patientId)) {
        throw new PatientInUseException(
            "This patient has appointments or medical records and can't be deleted.");
    }
    patientDAO.delete(patientId);
}
```

## 4. `PatientRestController` — add two endpoints

```java
@PutMapping("/{id}")
public Patient update(@PathVariable int id, @RequestBody Patient patient) {
    patient.setPatientId(id);
    patientService.update(patient);   // validates first/last name, updates contact fields
    return patientService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
}

@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable int id) {
    patientService.delete(id);
}
```

## 5. Appointments

**`AppointmentDAO`** — add three methods:

```java
/** Like hasConflict, but ignores the appointment being edited. */
public boolean hasConflictExcluding(int doctorId, LocalDateTime time, int excludeAppointmentId) {
    String sql = "SELECT 1 FROM appointments WHERE doctor_id=? AND appointment_time=? " +
                 "AND appointment_id<>? AND status NOT IN ('CANCELLED','NO_SHOW')";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, doctorId);
        ps.setTimestamp(2, Timestamp.valueOf(time));
        ps.setInt(3, excludeAppointmentId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next();
        }
    } catch (SQLException e) {
        throw new DatabaseException("Failed to check appointment conflicts", e);
    }
}

public void update(int appointmentId, LocalDateTime time, String reason) {
    String sql = "UPDATE appointments SET appointment_time=?, reason=? WHERE appointment_id=?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setTimestamp(1, Timestamp.valueOf(time));
        ps.setString(2, reason);
        ps.setInt(3, appointmentId);
        ps.executeUpdate();
    } catch (SQLException e) {
        throw new DatabaseException("Failed to update appointment", e);
    }
}

public void delete(int appointmentId) {
    String sql = "DELETE FROM appointments WHERE appointment_id=?";
    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setInt(1, appointmentId);
        ps.executeUpdate();
    } catch (SQLException e) {
        throw new DatabaseException("Failed to delete appointment", e);
    }
}
```

**`AppointmentService`** — add two methods (`requireExisting` is already in the class):

```java
public Appointment reschedule(int appointmentId, LocalDateTime time, String reason) {
    Appointment appt = requireExisting(appointmentId);
    if (appt.getStatus() != Appointment.Status.SCHEDULED) {
        throw new InvalidAppointmentException("Only scheduled appointments can be changed");
    }
    // Only insist on a future time if the time is actually being changed, so the reason
    // of an appointment earlier today can still be edited. The doctor stays the same.
    boolean timeChanged = !time.equals(appt.getAppointmentTime());
    if (timeChanged && time.isBefore(LocalDateTime.now().minusMinutes(1))) {
        throw new InvalidAppointmentException("Appointment time must be in the future");
    }
    if (appointmentDAO.hasConflictExcluding(appt.getDoctorId(), time, appointmentId)) {
        throw new InvalidAppointmentException("This doctor already has an appointment at that time");
    }
    appointmentDAO.update(appointmentId, time, reason);
    return requireExisting(appointmentId);
}

public void delete(int appointmentId) {
    Appointment appt = requireExisting(appointmentId);
    if (appt.getStatus() != Appointment.Status.CANCELLED) {
        throw new InvalidAppointmentException("Only cancelled appointments can be deleted. Cancel it first.");
    }
    appointmentDAO.delete(appointmentId);
}
```

**`AppointmentQueueRestController`** — add one record and two endpoints:

```java
record UpdateRequest(LocalDateTime appointmentTime, String reason) {}

@PutMapping("/appointments/{id}")
public Appointment update(@PathVariable int id, @RequestBody UpdateRequest r) {
    return appointmentService.reschedule(id, r.appointmentTime(), r.reason());
}

@DeleteMapping("/appointments/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable int id) {
    appointmentService.delete(id);
}
```

## 6. Error messages — `ApiExceptionHandler`

Without this, a refused action shows "Internal Server Error" instead of the real reason (for example
"This patient has appointments or medical records and can't be deleted"). If you already created one, add
`PatientInUseException` to its 409 list; otherwise add this class (e.g. `controller/ApiExceptionHandler.java`):

```java
package com.mediflow.controller;

import com.mediflow.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static ResponseEntity<Map<String, String>> reply(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    @ExceptionHandler({DuplicatePatientException.class, InvalidAppointmentException.class,
            InsufficientStockException.class, ExpiredMedicineException.class,
            InvalidPrescriptionException.class, PatientInUseException.class})
    ResponseEntity<Map<String, String>> conflict(RuntimeException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String, String>> unauthorised(RuntimeException e) {
        return reply(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return reply(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
```

---

## Try it

1. Restart the app and open `http://localhost:8080/`.
2. **Patients:** click the pencil on a patient, change a phone number, save. Then click the bin on a patient who has no
   appointments: it is removed. Try the bin on a patient with an appointment: the dialog stays open and shows the reason.
3. **Appointments:** click the pencil on a scheduled appointment, move it to a time another appointment already uses for that
   doctor: you get "This doctor already has an appointment at that time".
4. Type in the search box at the top of Patients or Appointments: the list narrows as you type.

## Who can do this

For now any signed-in user can call these endpoints. Before real use, restrict patient and appointment changes to
receptionists (and delete to a smaller group) on the server by checking the `User` you stored in the session at login.
