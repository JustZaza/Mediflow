package com.mediflow.service;

import com.mediflow.dao.DispensingDAO;
import com.mediflow.dao.InventoryDAO;
import com.mediflow.dao.PrescriptionDAO;
import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.exception.ExpiredMedicineException;
import com.mediflow.exception.InsufficientStockException;
import com.mediflow.exception.InvalidPrescriptionException;
import com.mediflow.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * FR-07/FR-08: pharmacy inventory + dispensing.
 * Core rules enforced here (Section 6 & 14):
 *   - stock cannot go negative
 *   - dispensing quantity cannot exceed available stock
 *   - expired medicine cannot be dispensed
 *   - dispensing is performed as a single database transaction
 * Batches are consumed FEFO (first-expiry-first-out) across possibly multiple batches per item.
 */
public class PharmacyService {
    private final PrescriptionDAO prescriptionDAO;
    private final DispensingDAO dispensingDAO;
    private final InventoryDAO inventoryDAO;

    public PharmacyService(PrescriptionDAO prescriptionDAO, DispensingDAO dispensingDAO, InventoryDAO inventoryDAO) {
        this.prescriptionDAO = prescriptionDAO;
        this.dispensingDAO = dispensingDAO;
        this.inventoryDAO = inventoryDAO;
    }

    public Dispensing dispense(int prescriptionId, User pharmacist) {
        if (pharmacist.getRole() != User.Role.PHARMACIST) {
            throw new InvalidPrescriptionException("Only pharmacists can dispense prescriptions");
        }
        Optional<Prescription> found = prescriptionDAO.findById(prescriptionId);
        if (found.isEmpty()) throw new InvalidPrescriptionException("Prescription not found: " + prescriptionId);
        Prescription prescription = found.get();
        if (prescription.getStatus() == Prescription.Status.DISPENSED) {
            throw new InvalidPrescriptionException("This prescription has already been fully dispensed");
        }
        if (prescription.getStatus() == Prescription.Status.CANCELLED) {
            throw new InvalidPrescriptionException("Cannot dispense a cancelled prescription");
        }

        LocalDate today = LocalDate.now();
        Dispensing dispensing = new Dispensing(prescriptionId, pharmacist.getUserId());

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dispensingDAO.insertHeader(conn, dispensing);

                for (PrescriptionItem item : prescription.getItems()) {
                    fulfillItem(conn, dispensing, item, today);
                }

                prescriptionDAO.updateStatus(conn, prescriptionId, Prescription.Status.DISPENSED);
                conn.commit();
                prescription.setStatus(Prescription.Status.DISPENSED);
                return dispensing;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                if (e instanceof RuntimeException re) throw re;
                throw new DatabaseException("Dispensing failed; all changes rolled back", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to open dispensing transaction", e);
        }
    }

    /** Fills one prescription item, possibly drawing from several non-expired batches, FEFO order. */
    private void fulfillItem(Connection conn, Dispensing dispensing, PrescriptionItem item, LocalDate today) throws SQLException {
        List<InventoryBatch> batches = inventoryDAO.findAvailableBatches(item.getMedicineId(), today);

        // findAvailableBatches already filters expiry_date >= today, but we double-check and
        // fail loudly per business rule "expired medicine must not be dispensed".
        batches.removeIf(b -> b.isExpired(today));

        int remaining = item.getQuantity();
        int totalAvailable = batches.stream().mapToInt(InventoryBatch::getQuantity).sum();
        if (totalAvailable < remaining) {
            throw new InsufficientStockException(
                    "Insufficient stock for " + item.getMedicineName() + ": need " + remaining + ", have " + totalAvailable);
        }
        if (batches.isEmpty()) {
            throw new ExpiredMedicineException("No non-expired stock available for " + item.getMedicineName());
        }

        for (InventoryBatch batch : batches) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, batch.getQuantity());

            inventoryDAO.deductQuantity(conn, batch.getInventoryId(), take);
            inventoryDAO.recordMovement(conn, batch.getInventoryId(), "DISPENSE", -take,
                    "prescription_id=" + item.getPrescriptionId());

            DispensingItem di = new DispensingItem(item.getMedicineId(), batch.getInventoryId(), take);
            dispensingDAO.insertItem(conn, dispensing.getDispensingId(), di);
            dispensing.addItem(di);

            remaining -= take;
        }
    }

    public List<InventoryBatch> lowStockReport() { return inventoryDAO.findLowStock(); }

    public List<InventoryBatch> expiringSoonReport(int withinDays) { return inventoryDAO.findExpiringSoon(withinDays); }

    public InventoryBatch receiveStock(InventoryBatch batch) {
        InventoryBatch saved = inventoryDAO.insert(batch);
        try (Connection conn = DatabaseConnection.getConnection()) {
            inventoryDAO.recordMovement(conn, saved.getInventoryId(), "RECEIVE", saved.getQuantity(),
                    "batch=" + saved.getBatchNumber());
        } catch (SQLException e) {
            throw new DatabaseException("Failed to record stock receipt movement", e);
        }
        return saved;
    }
}
