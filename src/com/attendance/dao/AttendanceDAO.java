package com.attendance.dao;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import java.time.LocalDate;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttendanceDAO {

    public boolean markAttendance(String studentId, String courseCode, AttendanceStatus status, LocalDate sessionDate) {
        if (studentId == null || courseCode == null || status == null || sessionDate == null) {
            return false;
        }
        String sql = "INSERT INTO attendance_records (record_id, student_id, course_code, session_date, recorded_at, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "REC-" + UUID.randomUUID().toString().substring(0, 6));
            pstmt.setString(2, studentId);
            pstmt.setString(3, courseCode.toUpperCase().trim());
            pstmt.setString(4, sessionDate.toString());
            pstmt.setString(5, LocalDateTime.now().toString());
            pstmt.setString(6, status.name());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Failed to record attendance: " + e.getMessage());
            return false;
        }
    }

    public List<AttendanceRecord> findByStudent(String studentId) {
        List<AttendanceRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM attendance_records WHERE student_id = ? ORDER BY session_date DESC, recorded_at DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                records.add(new AttendanceRecord(
                        rs.getString("record_id"),
                        rs.getString("student_id"),
                        rs.getString("course_code"),
                        AttendanceStatus.valueOf(rs.getString("status")),
                        LocalDate.parse(rs.getString("session_date"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Error fetching attendance records: " + e.getMessage());
        }
        return records;
    }
    public boolean enrollStudent(String studentId, String courseCode) {
    if (studentId == null || courseCode == null || studentId.isBlank() || courseCode.isBlank()) {
        return false;
    }
    String sql = "INSERT INTO enrollments (student_id, course_code) VALUES (?, ?)";
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, studentId);
        pstmt.setString(2, courseCode.toUpperCase().trim());
        pstmt.executeUpdate();
        return true;
    } catch (SQLException e) {
        if (!e.getMessage().contains("UNIQUE constraint failed")) {
            System.err.println("[DAO ERROR] Enrollment failed: " + e.getMessage());
        }
        return false;
    }
    }

    public boolean isStudentEnrolledInCourse(String studentId, String courseCode) {
        if (studentId == null || courseCode == null) {
            return false;
        }
        String sql = "SELECT 1 FROM enrollments WHERE student_id = ? AND course_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId.trim());
            pstmt.setString(2, courseCode.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Enrollment lookup failed: " + e.getMessage());
            return false;
        }
    }
}