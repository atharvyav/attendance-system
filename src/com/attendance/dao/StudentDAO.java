package com.attendance.dao;

import com.attendance.model.Student;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDAO {

    public boolean save(Student student) {
        String sql = "INSERT INTO students (student_id, full_name, roll_number) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getId());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getRollNo());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                System.err.println("[DAO ERROR] Failed to save student: " + e.getMessage());
            }
            return false;
        }
    }

    public Optional<Student> findById(String id) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Student(
                        rs.getString("student_id"),
                        rs.getString("full_name"),
                        rs.getString("roll_number")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Error finding student by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Student> findAll() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                students.add(new Student(
                        rs.getString("student_id"),
                        rs.getString("full_name"),
                        rs.getString("roll_number")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Error retrieving students: " + e.getMessage());
        }
        return students;
    }
    public String generateNextStudentId() {
    String sql = "SELECT student_id FROM students";
    int maxId = 1000; // Baseline start ID (so next is STU-1001)

    try (Connection conn = DatabaseManager.getConnection();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            String idStr = rs.getString("student_id"); // e.g., "STU-1003"
            if (idStr != null && idStr.startsWith("STU-")) {
                try {
                    int num = Integer.parseInt(idStr.replace("STU-", "").trim());
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    } catch (SQLException e) {
        System.err.println("[DAO ERROR] Error calculating next student ID: " + e.getMessage());
    }

    return "STU-" + (maxId + 1);
}
}
