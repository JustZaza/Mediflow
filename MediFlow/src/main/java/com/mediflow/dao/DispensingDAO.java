package com.mediflow.dao;

import com.mediflow.model.Dispensing;
import com.mediflow.model.DispensingItem;

import java.sql.*;

/**
 * Unlike the other DAOs, insert() here takes an existing Connection because dispensing must
 * run as a single transaction together with the inventory deduction (business rule: "database
 * changes involved in dispensing should be performed as a transaction"). See PharmacyService.
 */
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
}
