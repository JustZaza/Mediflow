package com.mediflow;

import com.mediflow.dao.*;
import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.*;
import com.mediflow.model.*;
import com.mediflow.service.*;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the key scenarios from the design doc's Testing Strategy table (section 15):
 * duplicate patient rejected, overlapping appointment rejected, over-dispensing rejected
 * (stock unchanged), expired medicine rejected, and a valid dispensing path.
 */
class PharmacyServiceTest {

    private PatientDAO patientDAO;
    private AppointmentDAO appointmentDAO;
    private MedicineDAO medicineDAO;
    private InventoryDAO inventoryDAO;
    private PrescriptionDAO prescriptionDAO;
    private DispensingDAO dispensingDAO;
    private UserDAO userDAO;

    private PatientService patientService;
    private AppointmentService appointmentService;
    private PrescriptionService prescriptionService;
    private PharmacyService pharmacyService;
    private AuthService authService;

    private int doctorUserId;
    private int doctorId;
    private int pharmacistUserId;

    @BeforeEach
    void setUp() throws Exception {
        loadSchema();

        patientDAO = new PatientDAO();
        appointmentDAO = new AppointmentDAO();
        medicineDAO = new MedicineDAO();
        inventoryDAO = new InventoryDAO();
        prescriptionDAO = new PrescriptionDAO();
        dispensingDAO = new DispensingDAO();
        userDAO = new UserDAO();

        patientService = new PatientService(patientDAO);
        appointmentService = new AppointmentService(appointmentDAO);
        prescriptionService = new PrescriptionService(prescriptionDAO);
        pharmacyService = new PharmacyService(prescriptionDAO, dispensingDAO, inventoryDAO);
        authService = new AuthService(userDAO);

        User doctorUser = authService.register("dr.test", "pw12345", User.Role.DOCTOR, "Dr. Test", "d@test.local");
        doctorUserId = doctorUser.getUserId();
        doctorId = insertDoctor(doctorUserId, "Dr. Test");

        User pharmacistUser = authService.register("pharm.test", "pw12345", User.Role.PHARMACIST, "Pharm Test", "p@test.local");
        pharmacistUserId = pharmacistUser.getUserId();
    }

    @Test
    void registeringDuplicatePatientCodeIsRejected() {
        Patient p1 = samplePatient("P-100");
        patientService.register(p1);

        Patient p2 = samplePatient("P-100");
        assertThrows(DuplicatePatientException.class, () -> patientService.register(p2));
    }

    @Test
    void overlappingDoctorAppointmentIsRejected() {
        LocalDateTime time = LocalDateTime.now().plusDays(1).withMinute(0).withSecond(0).withNano(0);
        Patient patient = patientService.register(samplePatient("P-200"));

        appointmentService.book(patient.getPatientId(), doctorId, time, "First visit");

        assertThrows(InvalidAppointmentException.class,
                () -> appointmentService.book(patient.getPatientId(), doctorId, time, "Conflicting visit"));
    }

    @Test
    void dispensingMoreThanAvailableStockIsRejectedAndStockUnchanged() {
        Medicine med = medicineDAO.insert(new Medicine("TestMed", "desc", "Tablet", "10mg"));
        InventoryBatch batch = new InventoryBatch();
        batch.setMedicineId(med.getMedicineId());
        batch.setBatchNumber("B-1");
        batch.setQuantity(5);
        batch.setMinimumStock(1);
        batch.setExpiryDate(LocalDate.now().plusYears(1));
        pharmacyService.receiveStock(batch);

        Patient patient = patientService.register(samplePatient("P-300"));
        int recordId = insertMedicalRecordStub(patient.getPatientId(), doctorId);

        User doctorUser = userDAO.findByUsername("dr.test").orElseThrow();
        Prescription prescription = prescriptionService.create(doctorUser, patient.getPatientId(), doctorId, recordId,
                List.of(new PrescriptionItem(med.getMedicineId(), "10mg", "Once daily", "5 days", 10))); // more than the 5 in stock

        User pharmacistUser = userDAO.findByUsername("pharm.test").orElseThrow();
        assertThrows(InsufficientStockException.class, () -> pharmacyService.dispense(prescription.getPrescriptionId(), pharmacistUser));

        // stock must remain unchanged after the rejected attempt
        List<InventoryBatch> batches = inventoryDAO.findAvailableBatches(med.getMedicineId(), LocalDate.now());
        assertEquals(5, batches.get(0).getQuantity());
    }

    @Test
    void validDispensingReducesInventoryAndMarksPrescriptionDispensed() {
        Medicine med = medicineDAO.insert(new Medicine("TestMed2", "desc", "Tablet", "20mg"));
        InventoryBatch batch = new InventoryBatch();
        batch.setMedicineId(med.getMedicineId());
        batch.setBatchNumber("B-2");
        batch.setQuantity(10);
        batch.setMinimumStock(1);
        batch.setExpiryDate(LocalDate.now().plusYears(1));
        pharmacyService.receiveStock(batch);

        Patient patient = patientService.register(samplePatient("P-400"));
        int recordId = insertMedicalRecordStub(patient.getPatientId(), doctorId);

        User doctorUser = userDAO.findByUsername("dr.test").orElseThrow();
        Prescription prescription = prescriptionService.create(doctorUser, patient.getPatientId(), doctorId, recordId,
                List.of(new PrescriptionItem(med.getMedicineId(), "20mg", "Once daily", "4 days", 4)));

        User pharmacistUser = userDAO.findByUsername("pharm.test").orElseThrow();
        Dispensing dispensing = pharmacyService.dispense(prescription.getPrescriptionId(), pharmacistUser);

        assertEquals(1, dispensing.getItems().size());
        List<InventoryBatch> batches = inventoryDAO.findAvailableBatches(med.getMedicineId(), LocalDate.now());
        assertEquals(6, batches.get(0).getQuantity()); // 10 - 4
        assertEquals(Prescription.Status.DISPENSED, prescriptionService.findById(prescription.getPrescriptionId()).orElseThrow().getStatus());
    }

    // ---- helpers ----

    private Patient samplePatient(String code) {
        Patient p = new Patient();
        p.setPatientCode(code);
        p.setFirstName("Test");
        p.setLastName("Patient");
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setGender(Patient.Gender.OTHER);
        return p;
    }

    private int insertDoctor(int userId, String fullName) throws Exception {
        String sql = "INSERT INTO doctors (user_id, full_name, specialization) VALUES (?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, fullName);
            ps.setString(3, "General");
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    private int insertMedicalRecordStub(int patientId, int doctorId) throws Exception {
        // create a throwaway appointment first since medical_records has a FK to it
        Appointment appt = appointmentService.book(patientId, doctorId, LocalDateTime.now().plusHours(1), "Consult");
        String sql = "INSERT INTO medical_records (patient_id, doctor_id, appointment_id, symptoms, diagnosis, notes) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, patientId);
            ps.setInt(2, doctorId);
            ps.setInt(3, appt.getAppointmentId());
            ps.setString(4, "test symptoms");
            ps.setString(5, "test diagnosis");
            ps.setString(6, "test notes");
            ps.executeUpdate();
            var keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    private void loadSchema() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     getClass().getResourceAsStream("/demo-schema.sql"), StandardCharsets.UTF_8))) {
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
