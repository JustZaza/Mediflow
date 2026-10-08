package com.mediflow.model;

import java.time.LocalDate;

/** One batch of stock for a medicine (named InventoryBatch to keep it readable vs. the `inventory` table). */
public class InventoryBatch {
    private int inventoryId;
    private int medicineId;
    private Integer supplierId;
    private String batchNumber;
    private int quantity;
    private int minimumStock;
    private LocalDate expiryDate;
    private String medicineName; // convenience field for display, populated by DAO joins

    public InventoryBatch() {}

    public boolean isLowStock() { return quantity <= minimumStock; }
    public boolean isExpired(LocalDate asOf) { return expiryDate != null && expiryDate.isBefore(asOf); }

    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }
    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }
    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getMinimumStock() { return minimumStock; }
    public void setMinimumStock(int minimumStock) { this.minimumStock = minimumStock; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
}
