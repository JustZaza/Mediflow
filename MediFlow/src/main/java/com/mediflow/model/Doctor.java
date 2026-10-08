package com.mediflow.model;

public class Doctor {
    private int doctorId;
    private int userId;
    private String fullName;
    private String specialization;

    public Doctor() {}

    public Doctor(int userId, String fullName, String specialization) {
        this.userId = userId;
        this.fullName = fullName;
        this.specialization = specialization;
    }

    public int getDoctorId() { return doctorId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
}
