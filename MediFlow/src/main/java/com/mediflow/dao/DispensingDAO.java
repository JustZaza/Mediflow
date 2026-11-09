package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.Dispensing;
import com.mediflow.model.DispensingItem;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;


/**
 * Unlike the other DAOs, insert() here takes an existing Connection because dispensing must
 * run as a single transaction together with the inventory deduction (business rule: "database
 * changes involved in dispensing should be performed as a transaction"). See PharmacyService.
 */
@Repository
public class DispensingDAO {

    public Dispensing insertHeader(Connection conn, Dispensing d) throws SQLException {
        String sql = "INSERT INTO dispensing (prescription_id, pharmacist_id) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, d.getPrescriptionId());
            ps.setInt(2, d.getPharmacistUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) d.setDispensingId(keys.getInt(1));
            }
        }
        return d;
    }

    public void insertItem(Connection conn, int dispensingId, DispensingItem item) throws SQLException {
        String sql = "INSERT INTO dispensing_items (dispensing_id, medicine_id, inventory_id, quantity) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, dispensingId);
            ps.setInt(2, item.getMedicineId());
            ps.setInt(3, item.getInventoryId());
            ps.setInt(4, item.getQuantity());
            ps.executeUpdate();
        }
    }

    public List<Dispensing> findAll() {
        String sql = "SELECT * FROM dispensing ORDER BY dispensed_at DESC";
        String itemSql = "SELECT * FROM dispensing_items";
        List<Dispensing> results = new ArrayList<>();
        Map<Integer, Dispensing> byId = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Dispensing d = new Dispensing();
                    d.setDispensingId(rs.getInt("dispensing_id"));
                    d.setPrescriptionId(rs.getInt("prescription_id"));
                    d.setPharmacistUserId(rs.getInt("pharmacist_id"));
                    Timestamp ts = rs.getTimestamp("dispensed_at");
                    d.setDispensedAt(ts != null ? ts.toLocalDateTime() : null);
                    results.add(d);
                    byId.put(d.getDispensingId(), d);
                }
            }
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(itemSql)) {
                while (rs.next()) {
                    Dispensing d = byId.get(rs.getInt("dispensing_id"));
                    if (d == null) continue;
                    DispensingItem it = new DispensingItem();
                    it.setDispensingItemId(rs.getInt("dispensing_item_id"));
                    it.setDispensingId(d.getDispensingId());
                    it.setMedicineId(rs.getInt("medicine_id"));
                    it.setInventoryId(rs.getInt("inventory_id"));
                    it.setQuantity(rs.getInt("quantity"));
                    d.addItem(it);
                }
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load dispensing history", e);
        }
    }
}
