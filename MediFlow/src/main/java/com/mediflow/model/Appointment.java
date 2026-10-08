package com.mediflow.model;

import java.time.LocalDateTime;

public class Appointment {
    public enum Status { SCHEDULED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW }

    private int appointmentId;
    private int patientId;
    private int doctorId;
    private LocalDateTime appointmentTime;
    private String reason;
    private Status status = Status.SCHEDULED;

    public Appointment() {}

    public Appointment(int patientId, int doctorId, LocalDateTime appointmentTime, String reason) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
    }

    public int getAppointmentId() { return appointmentId; }
    public void setAppointmentId(int appointmentId) { this.appointmentId = appointmentId; }
    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public LocalDateTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalDateTime appointmentTime) { this.appointmentTime = appointmentTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
