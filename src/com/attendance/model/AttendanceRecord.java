package com.attendance.model;

import java.time.LocalDate;

public class AttendanceRecord {
    private final String id;
    private final String studentId;
    private final String courseCode;
    private final AttendanceStatus status;
    private final LocalDate sessionDate;

    public AttendanceRecord(String id, String studentId, String courseCode, AttendanceStatus status, LocalDate sessionDate) {
        this.id = id;
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.status = status;
        this.sessionDate = sessionDate;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getCourseCode() { return courseCode; }
    public AttendanceStatus getStatus() { return status; }
    public LocalDate getSessionDate() { return sessionDate; }
}