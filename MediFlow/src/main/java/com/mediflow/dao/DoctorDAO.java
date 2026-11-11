package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.Doctor;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DoctorDAO {
    public List<Doctor> findAll() {
        String sql = "SELECT doctor_id, user_id, full_name, specialization FROM doctors ORDER BY full_name";
        List<Doctor> out = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Doctor d = new Doctor();
                d.setDoctorId(rs.getInt("doctor_id"));
                d.setUserId(rs.getInt("user_id"));
                d.setFullName(rs.getString("full_name"));
                d.setSpecialization(rs.getString("specialization"));
                out.add(d);
            }
            return out;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list doctors", e);
        }
    }

    public static java.util.Optional<Doctor> findByUserId(int userId) {
        String sql = "SELECT doctor_id, user_id, full_name, specialization FROM doctors WHERE user_id = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                Doctor d = new Doctor();
                d.setDoctorId(rs.getInt("doctor_id"));
                d.setUserId(rs.getInt("user_id"));
                d.setFullName(rs.getString("full_name"));
                d.setSpecialization(rs.getString("specialization"));
                return java.util.Optional.of(d);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find doctor", e);
        }
    }
}