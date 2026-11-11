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