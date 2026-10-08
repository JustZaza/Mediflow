package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.Patient;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientDAO {

    public Patient insert(Patient p) {
        String sql = "INSERT INTO patients (patient_code, first_name, last_name, date_of_birth, gender, phone, " +
                "email, address, emergency_contact_name, emergency_contact_phone) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getPatientCode());
            ps.setString(2, p.getFirstName());
            ps.setString(3, p.getLastName());
            ps.setDate(4, Date.valueOf(p.getDateOfBirth()));
            ps.setString(5, p.getGender().name());
            ps.setString(6, p.getPhone());
            ps.setString(7, p.getEmail());
            ps.setString(8, p.getAddress());
            ps.setString(9, p.getEmergencyContactName());
            ps.setString(10, p.getEmergencyContactPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setPatientId(keys.getInt(1));
            }
            return p;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert patient", e);
        }
    }

    public boolean existsByCode(String patientCode) {
        String sql = "SELECT 1 FROM patients WHERE patient_code = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check patient code uniqueness", e);
        }
    }

    public Optional<Patient> findById(int patientId) {
        String sql = "SELECT * FROM patients WHERE patient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find patient", e);
        }
    }

    public List<Patient> search(String nameFragment) {
        String sql = "SELECT * FROM patients WHERE first_name LIKE ? OR last_name LIKE ? OR patient_code LIKE ? ORDER BY last_name";
        String like = "%" + nameFragment + "%";
        List<Patient> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search patients", e);
        }
    }

    public void update(Patient p) {
        String sql = "UPDATE patients SET first_name=?, last_name=?, phone=?, email=?, address=?, " +
                "emergency_contact_name=?, emergency_contact_phone=? WHERE patient_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getFirstName());
            ps.setString(2, p.getLastName());
            ps.setString(3, p.getPhone());
            ps.setString(4, p.getEmail());
            ps.setString(5, p.getAddress());
            ps.setString(6, p.getEmergencyContactName());
            ps.setString(7, p.getEmergencyContactPhone());
            ps.setInt(8, p.getPatientId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update patient", e);
        }
    }

    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setPatientId(rs.getInt("patient_id"));
        p.setPatientCode(rs.getString("patient_code"));
        p.setFirstName(rs.getString("first_name"));
        p.setLastName(rs.getString("last_name"));
        Date dob = rs.getDate("date_of_birth");
        p.setDateOfBirth(dob != null ? LocalDate.parse(dob.toString()) : null);
        p.setGender(Patient.Gender.valueOf(rs.getString("gender")));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setAddress(rs.getString("address"));
        p.setEmergencyContactName(rs.getString("emergency_contact_name"));
        p.setEmergencyContactPhone(rs.getString("emergency_contact_phone"));
        return p;
    }
}
