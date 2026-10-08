package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.Prescription;
import com.mediflow.model.PrescriptionItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrescriptionDAO {

    /** Inserts the prescription and all of its items as one transaction (business rule: at least one item). */
    public Prescription insert(Prescription p) {
        String prescSql = "INSERT INTO prescriptions (patient_id, doctor_id, medical_record_id, status) VALUES (?,?,?,?)";
        String itemSql = "INSERT INTO prescription_items (prescription_id, medicine_id, dosage, frequency, duration, quantity) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(prescSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, p.getPatientId());
                    ps.setInt(2, p.getDoctorId());
                    ps.setInt(3, p.getMedicalRecordId());
                    ps.setString(4, p.getStatus().name());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) p.setPrescriptionId(keys.getInt(1));
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(itemSql, Statement.RETURN_GENERATED_KEYS)) {
                    for (PrescriptionItem item : p.getItems()) {
                        ps.setInt(1, p.getPrescriptionId());
                        ps.setInt(2, item.getMedicineId());
                        ps.setString(3, item.getDosage());
                        ps.setString(4, item.getFrequency());
                        ps.setString(5, item.getDuration());
                        ps.setInt(6, item.getQuantity());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                return p;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Failed to create prescription; rolled back", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to open transaction for prescription", e);
        }
    }

    public Optional<Prescription> findById(int prescriptionId) {
        String sql = "SELECT * FROM prescriptions WHERE prescription_id=?";
        String itemSql = "SELECT pi.*, m.name AS medicine_name FROM prescription_items pi " +
                "JOIN medicines m ON m.medicine_id = pi.medicine_id WHERE pi.prescription_id=?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            Prescription p;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, prescriptionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return Optional.empty();
                    p = mapRow(rs);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                ps.setInt(1, prescriptionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) p.addItem(mapItem(rs));
                }
            }
            return Optional.of(p);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load prescription", e);
        }
    }

    public List<Prescription> findPending() {
        String sql = "SELECT * FROM prescriptions WHERE status IN ('PENDING','PARTIALLY_DISPENSED') ORDER BY prescribed_at";
        List<Prescription> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(mapRow(rs));
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load pending prescriptions", e);
        }
    }

    public void updateStatus(Connection conn, int prescriptionId, Prescription.Status status) throws SQLException {
        String sql = "UPDATE prescriptions SET status=? WHERE prescription_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, prescriptionId);
            ps.executeUpdate();
        }
    }

    private Prescription mapRow(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setPrescriptionId(rs.getInt("prescription_id"));
        p.setPatientId(rs.getInt("patient_id"));
        p.setDoctorId(rs.getInt("doctor_id"));
        p.setMedicalRecordId(rs.getInt("medical_record_id"));
        Timestamp ts = rs.getTimestamp("prescribed_at");
        p.setPrescribedAt(ts != null ? ts.toLocalDateTime() : null);
        p.setStatus(Prescription.Status.valueOf(rs.getString("status")));
        return p;
    }

    private PrescriptionItem mapItem(ResultSet rs) throws SQLException {
        PrescriptionItem item = new PrescriptionItem();
        item.setItemId(rs.getInt("item_id"));
        item.setPrescriptionId(rs.getInt("prescription_id"));
        item.setMedicineId(rs.getInt("medicine_id"));
        item.setMedicineName(rs.getString("medicine_name"));
        item.setDosage(rs.getString("dosage"));
        item.setFrequency(rs.getString("frequency"));
        item.setDuration(rs.getString("duration"));
        item.setQuantity(rs.getInt("quantity"));
        return item;
    }
}
