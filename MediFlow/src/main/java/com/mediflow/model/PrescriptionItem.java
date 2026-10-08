package com.mediflow.model;

public class PrescriptionItem {
    private int itemId;
    private int prescriptionId;
    private int medicineId;
    private String medicineName; // convenience for display
    private String dosage;
    private String frequency;
    private String duration;
    private int quantity;

    public PrescriptionItem() {}

    public PrescriptionItem(int medicineId, String dosage, String frequency, String duration, int quantity) {
        this.medicineId = medicineId;
        this.dosage = dosage;
        this.frequency = frequency;
        this.duration = duration;
        this.quantity = quantity;
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
