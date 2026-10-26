package com.attendance.dao;

import com.attendance.model.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    public boolean save(Course course) {
        String sql = "INSERT INTO courses (course_code, course_title) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCode());
            pstmt.setString(2, course.getTitle());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                System.err.println("[DAO ERROR] Failed to save course: " + e.getMessage());
            }
            return false;
        }
    }

    public List<Course> findAll() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                courses.add(new Course(rs.getString("course_code"), rs.getString("course_title")));
            }
        } catch (SQLException e) {
            System.err.println("[DAO ERROR] Error retrieving courses: " + e.getMessage());
        }
        return courses;
    }
}