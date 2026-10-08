package com.mediflow.dao;

import com.mediflow.database.DatabaseConnection;
import com.mediflow.exception.DatabaseException;
import com.mediflow.model.User;

import java.sql.*;
import java.util.Optional;

/** All database access for `users`. GUI/service layers never write SQL directly (section 10). */
public class UserDAO {

    public User insert(User user) {
        String sql = "INSERT INTO users (username, password_hash, role, full_name, email, status) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setUserId(keys.getInt(1));
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert user", e);
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to look up user by username", e);
        }
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setStatus(User.Status.valueOf(rs.getString("status")));
        return u;
    }
}
