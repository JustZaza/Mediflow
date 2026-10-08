package com.mediflow.controller;

import com.mediflow.model.Dispensing;
import com.mediflow.model.InventoryBatch;
import com.mediflow.model.User;
import com.mediflow.service.PharmacyService;

import java.util.List;

public class PharmacyController {
    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) { this.pharmacyService = pharmacyService; }

    public Dispensing dispense(int prescriptionId, User pharmacist) { return pharmacyService.dispense(prescriptionId, pharmacist); }
    public List<InventoryBatch> lowStock() { return pharmacyService.lowStockReport(); }
    public List<InventoryBatch> expiringSoon(int withinDays) { return pharmacyService.expiringSoonReport(withinDays); }
    public InventoryBatch receiveStock(InventoryBatch batch) { return pharmacyService.receiveStock(batch); }
}
