package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.Appointment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentDAO {

    public Appointment insert(Appointment a) {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, appointment_time, reason, status) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getPatientId());
            ps.setInt(2, a.getDoctorId());
            ps.setTimestamp(3, Timestamp.valueOf(a.getAppointmentTime()));
            ps.setString(4, a.getReason());
            ps.setString(5, a.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) a.setAppointmentId(keys.getInt(1));
            }
            return a;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert appointment", e);
        }
    }

    /** True if the doctor already has a non-cancelled appointment at this exact time (business rule 6). */
    public boolean hasConflict(int doctorId, LocalDateTime time) {
        String sql = "SELECT 1 FROM appointments WHERE doctor_id=? AND appointment_time=? AND status NOT IN ('CANCELLED','NO_SHOW')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setTimestamp(2, Timestamp.valueOf(time));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check appointment conflicts", e);
        }
    }

    public void updateStatus(int appointmentId, Appointment.Status status) {
        String sql = "UPDATE appointments SET status=? WHERE appointment_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, appointmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update appointment status", e);
        }
    }

    public Optional<Appointment> findById(int appointmentId) {
        String sql = "SELECT * FROM appointments WHERE appointment_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, appointmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find appointment", e);
        }
    }

    public List<Appointment> findByDoctorAndDate(int doctorId, java.time.LocalDate date) {
        String sql = "SELECT * FROM appointments WHERE doctor_id=? AND CAST(appointment_time AS DATE)=? ORDER BY appointment_time";
        List<Appointment> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list doctor's appointments", e);
        }
    }

    private Appointment mapRow(ResultSet rs) throws SQLException {
        Appointment a = new Appointment();
        a.setAppointmentId(rs.getInt("appointment_id"));
        a.setPatientId(rs.getInt("patient_id"));
        a.setDoctorId(rs.getInt("doctor_id"));
        a.setAppointmentTime(rs.getTimestamp("appointment_time").toLocalDateTime());
        a.setReason(rs.getString("reason"));
        a.setStatus(Appointment.Status.valueOf(rs.getString("status")));
        return a;
    }
}
