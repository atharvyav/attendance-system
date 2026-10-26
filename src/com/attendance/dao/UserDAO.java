package com.attendance.dao;

import com.attendance.model.User;
import com.attendance.service.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UserDAO {

    public boolean save(String email, String password, String role, String studentId) {
        if (email == null || password == null || role == null || email.isBlank()
                || !PasswordHasher.meetsPolicy(password)
                || !("ADMIN".equals(role) || "STUDENT".equals(role))
                || ("STUDENT".equals(role) && studentId == null)) {
            return false;
        }
        String sql = "INSERT INTO users (user_id, email, password_hash, role, student_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "USR-" + UUID.randomUUID().toString().substring(0, 5));
            pstmt.setString(2, email.trim().toLowerCase());
            pstmt.setString(3, PasswordHasher.hash(password));
            pstmt.setString(4, role);
            pstmt.setString(5, studentId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                System.err.println("[DAO ERROR] Failed to save user: " + e.getMessage());
            }
            return false;
        }
    }

    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Email lookup failed: " + e.getMessage());
            return false;
        }
    }

    public boolean hasAdmin() throws SQLException {
        String sql = "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        }
    }

    public boolean removeKnownDefaultAdmin() throws SQLException {
        String sql = "DELETE FROM users WHERE LOWER(email) = ? AND role = 'ADMIN' AND password_hash = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "admin@srmist.edu.in");
            pstmt.setString(2, "admin123");
            return pstmt.executeUpdate() > 0;
        }
    }

    public Optional<User> findByEmailAndPassword(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }
        String sql = "SELECT user_id, email, password_hash, role, student_id FROM users WHERE LOWER(email) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim().toLowerCase());
            String userId;
            String storedValue;
            String storedEmail;
            String role;
            String studentId;
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                userId = rs.getString("user_id");
                storedValue = rs.getString("password_hash");
                storedEmail = rs.getString("email");
                role = rs.getString("role");
                studentId = rs.getString("student_id");
            }
            if (!PasswordHasher.verify(password, storedValue)) {
                return Optional.empty();
            }
            if (PasswordHasher.needsUpgrade(storedValue) && !upgradePassword(conn, userId, password)) {
                return Optional.empty();
            }
            return Optional.of(new User(userId, storedEmail, role, studentId));
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Login query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean changePassword(String userId, String currentPassword, String newPassword) {
        if (userId == null || currentPassword == null || !PasswordHasher.meetsPolicy(newPassword)) {
            return false;
        }
        String selectSql = "SELECT password_hash FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setString(1, userId);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next() || !PasswordHasher.verify(currentPassword, rs.getString("password_hash"))) {
                    return false;
                }
            }
            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                update.setString(1, PasswordHasher.hash(newPassword));
                update.setString(2, userId);
                return update.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Password change failed: " + e.getMessage());
            return false;
        }
    }

    private boolean upgradePassword(Connection conn, String userId, String password) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, PasswordHasher.hash(password));
            pstmt.setString(2, userId);
            return pstmt.executeUpdate() == 1;
        }
    }
}