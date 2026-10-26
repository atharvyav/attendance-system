package com.attendance.dao;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:attendance.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            return conn;
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
    }

    public static boolean initializeDatabase() {
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF;");
            }
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
            // 1. Students Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    student_id TEXT PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    roll_number TEXT NOT NULL
                );
            """);

            // 2. Courses Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS courses (
                    course_code TEXT PRIMARY KEY,
                    course_title TEXT NOT NULL
                );
            """);

            // 3. Users Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id TEXT PRIMARY KEY,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    role TEXT CHECK (role IN ('ADMIN', 'STUDENT')),
                    student_id TEXT,
                    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE SET NULL
                );
            """);

            // 4. Enrollments Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    student_id TEXT,
                    course_code TEXT,
                    PRIMARY KEY (student_id, course_code),
                    FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE CASCADE,
                    FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE
                );
            """);

            // 5. Attendance Records Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance_records (
                    record_id TEXT PRIMARY KEY,
                    student_id TEXT NOT NULL,
                    course_code TEXT NOT NULL,
                    session_date TEXT NOT NULL,
                    recorded_at TEXT NOT NULL,
                    status TEXT CHECK (status IN ('PRESENT', 'ABSENT', 'EXCUSED')),
                    FOREIGN KEY (student_id, course_code) REFERENCES enrollments(student_id, course_code)
                );
            """);
            }

            migrateStudentRollNumbers(conn);
            migrateAttendanceSessionDates(conn);
            conn.commit();
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            System.out.println("[DATABASE] SQLite initialized successfully.");
            return true;

        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Initialization failed: " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            System.err.println("[DATABASE ERROR] Initialization failed: " + e.getMessage());
            return false;
        }
    }

    private static void migrateStudentRollNumbers(Connection conn) throws SQLException {
        if (hasUniqueRollNumberIndex(conn)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE students_new (
                        student_id TEXT PRIMARY KEY,
                        full_name TEXT NOT NULL,
                        roll_number TEXT NOT NULL
                    );
                    """);
                stmt.execute("""
                    INSERT INTO students_new (student_id, full_name, roll_number)
                    SELECT student_id, full_name, roll_number FROM students;
                    """);
                stmt.execute("DROP TABLE students;");
                stmt.execute("ALTER TABLE students_new RENAME TO students;");
            }
        }

        String selectSql = "SELECT student_id FROM students";
        String updateSql = "UPDATE students SET roll_number = ? WHERE student_id = ?";
        try (Statement select = conn.createStatement();
             ResultSet rs = select.executeQuery(selectSql);
             PreparedStatement update = conn.prepareStatement(updateSql)) {
            while (rs.next()) {
                String studentId = rs.getString("student_id");
                try {
                    update.setString(1, com.attendance.model.Student.rollNumberForId(studentId));
                    update.setString(2, studentId);
                    update.addBatch();
                } catch (IllegalArgumentException e) {
                    System.err.println("[DATABASE WARNING] Keeping existing roll number for invalid student ID: " + studentId);
                }
            }
            update.executeBatch();
        }
    }

    private static boolean hasUniqueRollNumberIndex(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet indexes = stmt.executeQuery("PRAGMA index_list('students')")) {
            while (indexes.next()) {
                if (indexes.getInt("unique") != 1) {
                    continue;
                }
                String indexName = indexes.getString("name").replace("\"", "\"\"");
                try (Statement indexStmt = conn.createStatement();
                     ResultSet columns = indexStmt.executeQuery("PRAGMA index_info(\"" + indexName + "\")")) {
                    if (columns.next() && "roll_number".equalsIgnoreCase(columns.getString("name"))
                            && !columns.next()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void migrateAttendanceSessionDates(Connection conn) throws SQLException {
        if (hasColumn(conn, "attendance_records", "session_date")) {
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE attendance_records_new (
                    record_id TEXT PRIMARY KEY,
                    student_id TEXT NOT NULL,
                    course_code TEXT NOT NULL,
                    session_date TEXT NOT NULL,
                    recorded_at TEXT NOT NULL,
                    status TEXT CHECK (status IN ('PRESENT', 'ABSENT', 'EXCUSED')),
                    FOREIGN KEY (student_id, course_code) REFERENCES enrollments(student_id, course_code)
                );
                """);
            stmt.execute("""
                INSERT INTO attendance_records_new
                    (record_id, student_id, course_code, session_date, recorded_at, status)
                SELECT record_id, student_id, course_code, substr(recorded_at, 1, 10), recorded_at, status
                FROM attendance_records;
                """);
            stmt.execute("DROP TABLE attendance_records;");
            stmt.execute("ALTER TABLE attendance_records_new RENAME TO attendance_records;");
        }
    }

    private static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        String safeTableName = table.replace("'", "''");
        try (Statement stmt = conn.createStatement();
             ResultSet columns = stmt.executeQuery("PRAGMA table_info('" + safeTableName + "')")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}