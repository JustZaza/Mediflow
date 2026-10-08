package com.mediflow.model;

import java.time.LocalDateTime;

public class MedicalRecord {
    private int recordId;
    private int patientId;
    private int doctorId;
    private int appointmentId;
    private String symptoms;
    private String diagnosis;
    private String notes;
    private LocalDateTime createdAt;

    public MedicalRecord() {}

    public MedicalRecord(int patientId, int doctorId, int appointmentId, String symptoms, String diagnosis, String notes) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentId = appointmentId;
        this.symptoms = symptoms;
        this.diagnosis = diagnosis;
        this.notes = notes;
    }

    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
