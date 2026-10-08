package com.mediflow.service;

import com.mediflow.dao.MedicalRecordDAO;
import com.mediflow.model.MedicalRecord;
import com.mediflow.util.ValidationUtil;

import java.util.List;

/** FR-05: doctors record symptoms/diagnosis/notes tied to a patient + appointment. */
public class MedicalRecordService {
    private final MedicalRecordDAO medicalRecordDAO;

    public MedicalRecordService(MedicalRecordDAO medicalRecordDAO) { this.medicalRecordDAO = medicalRecordDAO; }

    public MedicalRecord record(int patientId, int doctorId, int appointmentId, String symptoms, String diagnosis, String notes) {
        ValidationUtil.requirePositive(patientId, "Patient");
        ValidationUtil.requirePositive(doctorId, "Doctor");
        ValidationUtil.requirePositive(appointmentId, "Appointment");
        MedicalRecord record = new MedicalRecord(patientId, doctorId, appointmentId, symptoms, diagnosis, notes);
        return medicalRecordDAO.insert(record);
    }

    public List<MedicalRecord> historyFor(int patientId) {
        return medicalRecordDAO.findByPatient(patientId);
    }
}
