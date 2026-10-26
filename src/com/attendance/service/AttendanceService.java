package com.attendance.service;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.Student;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AttendanceService {

    public AttendanceRecord markAttendance(List<AttendanceRecord> records, String studentId, String courseCode, AttendanceStatus status) {
        if (records == null || studentId == null || courseCode == null || status == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        String recordId = "REC-" + UUID.randomUUID().toString().substring(0, 5);
        AttendanceRecord newRecord = new AttendanceRecord(recordId, studentId, courseCode, status, LocalDate.now());
        records.add(newRecord);
        return newRecord;
    }

    public double calculateAttendancePercentage(List<AttendanceRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0.0;
        }

        long presentCount = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
                .count();

        // Edge case handling: Ignore EXCUSED absences from total sessions denominator
        long eligibleSessionsCount = records.stream()
                .filter(r -> r.getStatus() != AttendanceStatus.EXCUSED)
                .count();

        if (eligibleSessionsCount == 0) {
            return 100.0; // Avoid divide-by-zero
        }

        return ((double) presentCount / eligibleSessionsCount) * 100.0;
    }

    public List<Student> getStudentsBelowThreshold(
            List<Student> students,
            List<AttendanceRecord> allRecords,
            String courseCode,
            double thresholdPercentage
    ) {
        if (students == null || allRecords == null || courseCode == null) {
            throw new IllegalArgumentException("Inputs cannot be null");
        }

        Map<String, List<AttendanceRecord>> recordsByStudent = allRecords.stream()
                .filter(r -> r.getCourseCode().equalsIgnoreCase(courseCode.trim()))
                .collect(Collectors.groupingBy(AttendanceRecord::getStudentId));

        return students.stream()
                .filter(student -> {
                    List<AttendanceRecord> studentRecords = recordsByStudent.getOrDefault(student.getId(), List.of());
                    double percentage = calculateAttendancePercentage(studentRecords);
                    return percentage < thresholdPercentage;
                })
                .collect(Collectors.toList());
    }
}