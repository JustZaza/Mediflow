package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.Medicine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MedicineDAO {

    public Medicine insert(Medicine m) {
        String sql = "INSERT INTO medicines (name, description, dosage_form, strength) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getDescription());
            ps.setString(3, m.getDosageForm());
            ps.setString(4, m.getStrength());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) m.setMedicineId(keys.getInt(1));
            }
            return m;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert medicine", e);
        }
    }

    public Optional<Medicine> findById(int medicineId) {
        String sql = "SELECT * FROM medicines WHERE medicine_id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, medicineId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find medicine", e);
        }
    }

    public List<Medicine> findAll() {
        String sql = "SELECT * FROM medicines ORDER BY name";
        List<Medicine> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) results.add(mapRow(rs));
            return results;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list medicines", e);
        }
    }

    private Medicine mapRow(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setMedicineId(rs.getInt("medicine_id"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setDosageForm(rs.getString("dosage_form"));
        m.setStrength(rs.getString("strength"));
        return m;
    }
}
