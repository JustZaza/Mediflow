package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.InventoryBatch;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public InventoryBatch insert(InventoryBatch b) {
        String sql = "INSERT INTO inventory (medicine_id, supplier_id, batch_number, quantity, minimum_stock, expiry_date) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getMedicineId());
            if (b.getSupplierId() != null) ps.setInt(2, b.getSupplierId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, b.getBatchNumber());
            ps.setInt(4, b.getQuantity());
            ps.setInt(5, b.getMinimumStock());
            ps.setDate(6, Date.valueOf(b.getExpiryDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) b.setInventoryId(keys.getInt(1));
            }
            return b;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert inventory batch", e);
        }
    }

    /** Non-expired batches for a medicine, oldest expiry first (dispense FEFO: first-expiry-first-out). */
    public List<InventoryBatch> findAvailableBatches(int medicineId, LocalDate asOf) {
        String sql = "SELECT i.*, m.name AS medicine_name FROM inventory i JOIN medicines m ON m.medicine_id = i.medicine_id " +
                "WHERE i.medicine_id=? AND i.expiry_date >= ? AND i.quantity > 0 ORDER BY i.expiry_date ASC";
        List<InventoryBatch> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            ps.setDate(2, Date.valueOf(asOf));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load available batches", e);
        }
    }

    /**
     * Deduct quantity from a batch as part of a caller-managed transaction (the connection's
     * auto-commit must already be false; see PharmacyService.dispense). Never lets stock go negative.
     */
    public void deductQuantity(Connection conn, int inventoryId, int quantity) throws SQLException {
        String sql = "UPDATE inventory SET quantity = quantity - ? WHERE inventory_id = ? AND quantity >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, inventoryId);
            ps.setInt(3, quantity);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new com.mediflow.exception.InsufficientStockException(
                        "Not enough stock in batch " + inventoryId + " for quantity " + quantity);
            }
        }
    }

    public void recordMovement(Connection conn, int inventoryId, String type, int quantity, String reference) throws SQLException {
        String sql = "INSERT INTO stock_movements (inventory_id, movement_type, quantity, reference) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, inventoryId);
            ps.setString(2, type);
            ps.setInt(3, quantity);
            ps.setString(4, reference);
            ps.executeUpdate();
        }
    }

    public List<InventoryBatch> findLowStock() {
        String sql = "SELECT i.*, m.name AS medicine_name FROM inventory i JOIN medicines m ON m.medicine_id = i.medicine_id " +
                "WHERE i.quantity <= i.minimum_stock";
        List<InventoryBatch> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(mapRow(rs));
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load low stock report", e);
        }
    }

    public List<InventoryBatch> findExpiringSoon(int days) {
        // Cutoff computed in Java (rather than a DB-specific date function) so this query
        // works unchanged against both MySQL and the H2 demo database.
        LocalDate cutoff = LocalDate.now().plusDays(days);
        String sql = "SELECT i.*, m.name AS medicine_name FROM inventory i JOIN medicines m ON m.medicine_id = i.medicine_id " +
                "WHERE i.expiry_date <= ?";
        List<InventoryBatch> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load expiring-soon report", e);
        }
    }

    private InventoryBatch mapRow(ResultSet rs) throws SQLException {
        InventoryBatch b = new InventoryBatch();
        b.setInventoryId(rs.getInt("inventory_id"));
        b.setMedicineId(rs.getInt("medicine_id"));
        int supplierId = rs.getInt("supplier_id");
        b.setSupplierId(rs.wasNull() ? null : supplierId);
        b.setBatchNumber(rs.getString("batch_number"));
        b.setQuantity(rs.getInt("quantity"));
        b.setMinimumStock(rs.getInt("minimum_stock"));
        Date exp = rs.getDate("expiry_date");
        b.setExpiryDate(exp != null ? LocalDate.parse(exp.toString()) : null);
        try { b.setMedicineName(rs.getString("medicine_name")); } catch (SQLException ignored) {}
        return b;
    }
}
