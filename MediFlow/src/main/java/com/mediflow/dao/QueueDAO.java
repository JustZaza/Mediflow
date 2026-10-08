package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.QueueEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QueueDAO {

    public QueueEntry insert(QueueEntry q) {
        String sql = "INSERT INTO queues (patient_id, appointment_id, priority, queue_number, status) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, q.getPatientId());
            ps.setInt(2, q.getAppointmentId());
            ps.setString(3, q.getPriority().name());
            ps.setInt(4, q.getQueueNumber());
            ps.setString(5, q.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setQueueId(keys.getInt(1));
            }
            return q;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert queue entry", e);
        }
    }

    public int nextQueueNumberForToday() {
        String sql = "SELECT COALESCE(MAX(queue_number),0)+1 FROM queues WHERE CAST(created_at AS DATE) = CURRENT_DATE";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to compute next queue number", e);
        }
    }

    /** Everyone still WAITING today, joined with patient name for display. */
    public List<QueueEntry> findWaitingToday() {
        String sql = "SELECT q.*, p.first_name, p.last_name FROM queues q " +
                "JOIN patients p ON p.patient_id = q.patient_id " +
                "WHERE q.status = 'WAITING' AND CAST(q.created_at AS DATE) = CURRENT_DATE";
        List<QueueEntry> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(mapRow(rs));
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to load today's queue", e);
        }
    }

    public void updateStatus(int queueId, QueueEntry.Status status) {
        String sql = "UPDATE queues SET status=? WHERE queue_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, queueId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update queue status", e);
        }
    }

    private QueueEntry mapRow(ResultSet rs) throws SQLException {
        QueueEntry q = new QueueEntry();
        q.setQueueId(rs.getInt("queue_id"));
        q.setPatientId(rs.getInt("patient_id"));
        q.setAppointmentId(rs.getInt("appointment_id"));
        q.setPriority(QueueEntry.Priority.valueOf(rs.getString("priority")));
        q.setQueueNumber(rs.getInt("queue_number"));
        q.setStatus(QueueEntry.Status.valueOf(rs.getString("status")));
        q.setPatientName(rs.getString("first_name") + " " + rs.getString("last_name"));
        return q;
    }
}
