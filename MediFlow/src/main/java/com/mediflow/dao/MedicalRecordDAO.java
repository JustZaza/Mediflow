package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.MedicalRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicalRecordDAO {

    public MedicalRecord insert(MedicalRecord r) {
        String sql = "INSERT INTO medical_records (patient_id, doctor_id, appointment_id, symptoms, diagnosis, notes) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getPatientId());
            ps.setInt(2, r.getDoctorId());
            ps.setInt(3, r.getAppointmentId());
            ps.setString(4, r.getSymptoms());
            ps.setString(5, r.getDiagnosis());
            ps.setString(6, r.getNotes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setRecordId(keys.getInt(1));
            }
            return r;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert medical record", e);
        }
    }

    public List<MedicalRecord> findByPatient(int patientId) {
        String sql = "SELECT * FROM medical_records WHERE patient_id=? ORDER BY created_at DESC";
        List<MedicalRecord> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load medical history", e);
        }
    }

    private MedicalRecord mapRow(ResultSet rs) throws SQLException {
        MedicalRecord r = new MedicalRecord();
        r.setRecordId(rs.getInt("record_id"));
        r.setPatientId(rs.getInt("patient_id"));
        r.setDoctorId(rs.getInt("doctor_id"));
        r.setAppointmentId(rs.getInt("appointment_id"));
        r.setSymptoms(rs.getString("symptoms"));
        r.setDiagnosis(rs.getString("diagnosis"));
        r.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        r.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return r;
    }
}
