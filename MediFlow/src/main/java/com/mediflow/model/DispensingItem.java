package com.mediflow.model;

public class DispensingItem {
    private int dispensingItemId;
    private int dispensingId;
    private int medicineId;
    private int inventoryId;
    private int quantity;

    public DispensingItem() {}

    public DispensingItem(int medicineId, int inventoryId, int quantity) {
        this.medicineId = medicineId;
        this.inventoryId = inventoryId;
        this.quantity = quantity;
    }

    public int getDispensingItemId() { return dispensingItemId; }
    public void setDispensingItemId(int dispensingItemId) { this.dispensingItemId = dispensingItemId; }
    public int getDispensingId() { return dispensingId; }
    public void setDispensingId(int dispensingId) { this.dispensingId = dispensingId; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
