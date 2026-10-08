package com.mediflow.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Prescription {
    public enum Status { PENDING, PARTIALLY_DISPENSED, DISPENSED, CANCELLED }

    private int prescriptionId;
    private int patientId;
    private int doctorId;
    private int medicalRecordId;
    private LocalDateTime prescribedAt;
    private Status status = Status.PENDING;
    private List<PrescriptionItem> items = new ArrayList<>();

    public Prescription() {}

    public Prescription(int patientId, int doctorId, int medicalRecordId) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.medicalRecordId = medicalRecordId;
    }

    public void addItem(PrescriptionItem item) { items.add(item); }

    public int getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public int getMedicalRecordId() { return medicalRecordId; }
    public void setMedicalRecordId(int medicalRecordId) { this.medicalRecordId = medicalRecordId; }
    public LocalDateTime getPrescribedAt() { return prescribedAt; }
    public void setPrescribedAt(LocalDateTime prescribedAt) { this.prescribedAt = prescribedAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) { this.items = items; }
}
