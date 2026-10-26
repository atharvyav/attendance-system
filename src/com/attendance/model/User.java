package com.attendance.model;

public class User {
    private final String id;
    private final String email;
    private final String role;
    private final String studentId;

    public User(String id, String email, String role, String studentId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.studentId = studentId;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStudentId() { return studentId; }
}