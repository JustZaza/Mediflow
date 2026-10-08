package com.mediflow.demo;

import com.mediflow.controller.*;
import com.mediflow.dao.*;
import com.mediflow.database.DatabaseConnection;
import com.mediflow.model.*;
import com.mediflow.service.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Runnable, end-to-end walk-through of the MVP workflow described in the design doc, section 20:
 *   Login -> Patient -> Appointment -> Queue -> Doctor consultation -> Prescription -> Pharmacy
 *   -> Dispensing -> Inventory update.
 */
public class MediFlowDemo {

    public static void main(String[] args) throws Exception {
        loadDemoSchema();

        // ---- wire up layers (DAO -> Service -> Controller), per the layered architecture ----
        UserDAO userDAO = new UserDAO();
        PatientDAO patientDAO = new PatientDAO();
        AppointmentDAO appointmentDAO = new AppointmentDAO();
        QueueDAO queueDAO = new QueueDAO();
        MedicalRecordDAO medicalRecordDAO = new MedicalRecordDAO();
        MedicineDAO medicineDAO = new MedicineDAO();
        InventoryDAO inventoryDAO = new InventoryDAO();
        PrescriptionDAO prescriptionDAO = new PrescriptionDAO();
        DispensingDAO dispensingDAO = new DispensingDAO();

        LoginController loginController = new LoginController(new AuthService(userDAO));
        PatientController patientController = new PatientController(new PatientService(patientDAO));
        AppointmentController appointmentController = new AppointmentController(new AppointmentService(appointmentDAO));
        QueueController queueController = new QueueController(new QueueService(queueDAO));
        PrescriptionController prescriptionController = new PrescriptionController(new PrescriptionService(prescriptionDAO));
        PharmacyController pharmacyController = new PharmacyController(new PharmacyService(prescriptionDAO, dispensingDAO, inventoryDAO));
        MedicalRecordService medicalRecordService = new MedicalRecordService(medicalRecordDAO);

        System.out.println("=== MediFlow MVP workflow demo ===\n");

        // ---- Setup: seed a doctor, a pharmacist, some medicines & stock ----
        AuthService authService = new AuthService(userDAO);
        User doctorUser = authService.register("dr.smith", "password123", User.Role.DOCTOR, "Dr. Alice Smith", "a.smith@mediflow.local");
        User pharmacistUser = authService.register("pharm1", "password123", User.Role.PHARMACIST, "David Okoye", "d.okoye@mediflow.local");
        Doctor doctor = new Doctor(doctorUser.getUserId(), doctorUser.getFullName(), "General Practice");
        int doctorId = insertDoctor(doctor);

        Medicine amoxicillin = medicineDAO.insert(new Medicine("Amoxicillin", "Antibiotic", "Capsule", "500mg"));
        InventoryBatch batch = new InventoryBatch();
        batch.setMedicineId(amoxicillin.getMedicineId());
        batch.setBatchNumber("AMX-DEMO-01");
        batch.setQuantity(20);
        batch.setMinimumStock(5);
        batch.setExpiryDate(LocalDate.now().plusYears(1));
        pharmacyController.receiveStock(batch);

        // ---- 1. Login ----
        User loggedInDoctor = loginController.login("dr.smith", "password123");
        System.out.println("[Login] " + loggedInDoctor.getFullName() + " logged in as " + loggedInDoctor.getRole());

        // ---- 2. Patient registration ----
        Patient patient = new Patient();
        patient.setPatientCode("P-DEMO-01");
        patient.setFirstName("Thabo");
        patient.setLastName("Nkosi");
        patient.setDateOfBirth(LocalDate.of(1988, 4, 12));
        patient.setGender(Patient.Gender.MALE);
        patient.setPhone("0821234567");
        patient = patientController.registerPatient(patient);
        System.out.println("[Patient] Registered " + patient.getFullName() + " (" + patient.getPatientCode() + ")");

        // ---- 3. Appointment ----
        Appointment appointment = appointmentController.book(
                patient.getPatientId(), doctorId, LocalDateTime.now().plusMinutes(5), "Persistent cough");
        System.out.println("[Appointment] Booked appointment #" + appointment.getAppointmentId()
                + " with Dr. Smith at " + appointment.getAppointmentTime());

        // ---- 4. Check-in / Queue ----
        appointmentController.checkIn(appointment.getAppointmentId());
        QueueEntry queueEntry = queueController.checkIn(patient.getPatientId(), appointment.getAppointmentId(), QueueEntry.Priority.NORMAL);
        System.out.println("[Queue] Checked in, queue number " + queueEntry.getQueueNumber() + " (priority " + queueEntry.getPriority() + ")");

        QueueEntry called = queueController.callNextPatient();
        System.out.println("[Queue] Doctor calls next patient: " + called.getPatientName());

        // ---- 5. Doctor consultation ----
        MedicalRecord record = medicalRecordService.record(
                patient.getPatientId(), doctorId, appointment.getAppointmentId(),
                "Persistent dry cough, mild fever", "Suspected bacterial bronchitis",
                "Prescribed antibiotics, review in 7 days if no improvement");
        System.out.println("[Consultation] Recorded diagnosis: " + record.getDiagnosis());

        // ---- 6. Prescription ----
        Prescription prescription = prescriptionController.create(
                loggedInDoctor, patient.getPatientId(), doctorId, record.getRecordId(),
                List.of(new PrescriptionItem(amoxicillin.getMedicineId(), "500mg", "Twice daily", "7 days", 14)));
        System.out.println("[Prescription] Created prescription #" + prescription.getPrescriptionId()
                + " for " + prescription.getItems().size() + " item(s), status " + prescription.getStatus());

        // ---- 7 & 8. Pharmacy: dispensing + inventory update (transactional) ----
        User loggedInPharmacist = loginController.login("pharm1", "password123");
        Dispensing dispensing = pharmacyController.dispense(prescription.getPrescriptionId(), loggedInPharmacist);
        System.out.println("[Dispensing] Dispensing #" + dispensing.getDispensingId()
                + " recorded by " + loggedInPharmacist.getFullName() + ", " + dispensing.getItems().size() + " line item(s)");

        List<InventoryBatch> remaining = pharmacyController.lowStock();
        System.out.println("[Inventory] Low-stock batches after dispensing: " + remaining.size());
        System.out.println("\n=== Workflow complete: Login -> Patient -> Appointment -> Queue -> "
                + "Consultation -> Prescription -> Dispensing -> Inventory update ===");

        // ---- Business-rule demo: try to over-dispense the same prescription again ----
        try {
            pharmacyController.dispense(prescription.getPrescriptionId(), loggedInPharmacist);
        } catch (RuntimeException e) {
            System.out.println("\n[Business rule check] Re-dispensing rejected as expected: " + e.getMessage());
        }
    }

    /** Minimal helper since DoctorDAO wasn't required for the MVP DAO list in the design doc's suggested structure. */
    private static int insertDoctor(Doctor doctor) throws Exception {
        String sql = "INSERT INTO doctors (user_id, full_name, specialization) VALUES (?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, doctor.getUserId());
            ps.setString(2, doctor.getFullName());
            ps.setString(3, doctor.getSpecialization());
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    private static void loadDemoSchema() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     MediFlowDemo.class.getResourceAsStream("/demo-schema.sql"), StandardCharsets.UTF_8))) {
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--") || line.isBlank()) continue;
                sql.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    st.execute(sql.toString());
                    sql.setLength(0);
                }
            }
        }
    }
}
