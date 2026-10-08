package com.mediflow.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Dispensing {
    private int dispensingId;
    private int prescriptionId;
    private int pharmacistUserId;
    private LocalDateTime dispensedAt;
    private List<DispensingItem> items = new ArrayList<>();

    public Dispensing() {}

    public Dispensing(int prescriptionId, int pharmacistUserId) {
        this.prescriptionId = prescriptionId;
        this.pharmacistUserId = pharmacistUserId;
    }

    public void addItem(DispensingItem item) { items.add(item); }

    public int getDispensingId() { return dispensingId; }
    public void setDispensingId(int dispensingId) { this.dispensingId = dispensingId; }
    public int getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }
    public int getPharmacistUserId() { return pharmacistUserId; }
    public void setPharmacistUserId(int pharmacistUserId) { this.pharmacistUserId = pharmacistUserId; }
    public LocalDateTime getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(LocalDateTime dispensedAt) { this.dispensedAt = dispensedAt; }
    public List<DispensingItem> getItems() { return items; }
    public void setItems(List<DispensingItem> items) { this.items = items; }
}
